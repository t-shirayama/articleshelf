package com.articleshelf.infrastructure.ogp;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class OgpClient {
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_BODY_BYTES = 1024 * 1024;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final OgpRequestGuard requestGuard = new OgpRequestGuard();

    public OgpMetadata fetch(String url) {
        try {
            URI requestUri = requestGuard.validate(url);
            HttpResponse<InputStream> response = sendFollowingRedirects(requestUri);
            if (response.statusCode() >= 400) {
                closeQuietly(response.body());
                return OgpMetadata.unavailable();
            }
            if (!isHtml(response)) {
                closeQuietly(response.body());
                return OgpMetadata.unavailable();
            }
            return new OgpHtmlParser().parse(
                    readLimited(response.body()),
                    response.headers().firstValue("Content-Type").orElse(""),
                    response.uri().toString()
            );
        } catch (IllegalArgumentException | OgpRequestGuard.UnsafeOgpUrlException ignored) {
            return OgpMetadata.unavailable();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return OgpMetadata.unavailable();
        } catch (IOException | UncheckedIOException ignored) {
            return OgpMetadata.unavailable();
        }
    }

    private HttpResponse<InputStream> sendFollowingRedirects(URI initialUri) throws IOException, InterruptedException {
        URI currentUri = initialUri;
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount += 1) {
            HttpRequest request = HttpRequest.newBuilder(requestGuard.validate(currentUri))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "ArticleShelf/0.1")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (!isRedirect(response.statusCode())) {
                return response;
            }
            if (redirectCount == MAX_REDIRECTS) {
                closeQuietly(response.body());
                throw new IOException("too many redirects");
            }
            String location = response.headers().firstValue("Location").orElseThrow(() -> new IOException("missing redirect location"));
            closeQuietly(response.body());
            currentUri = requestGuard.validate(response.uri().resolve(location));
        }
        throw new IOException("too many redirects");
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }

    private boolean isHtml(HttpResponse<?> response) {
        return response.headers().firstValue("Content-Type")
                .map(value -> value.toLowerCase().startsWith("text/html"))
                .orElse(false);
    }

    private byte[] readLimited(InputStream inputStream) throws IOException {
        try (inputStream) {
            byte[] bytes = inputStream.readNBytes(MAX_BODY_BYTES + 1);
            if (bytes.length > MAX_BODY_BYTES) {
                throw new IOException("response body is too large");
            }
            return bytes;
        }
    }

    private void closeQuietly(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException ignored) {
        }
    }

}
