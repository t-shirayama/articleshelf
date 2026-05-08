package com.readstack.infrastructure.ogp;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OgpClient {
    private static final Pattern META_TAG = Pattern.compile("<meta\\s+[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w:-]+)\\s*=\\s*([\"'])(.*?)\\2", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
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
            String body = readLimited(response.body());
            String title = extractMeta(body, "og:title").or(() -> extract(HTML_TITLE, body)).orElse("");
            String description = extractMeta(body, "og:description")
                    .or(() -> extractMeta(body, "description"))
                    .orElse("");
            String imageUrl = extractMeta(body, "og:image")
                    .or(() -> extractMeta(body, "twitter:image"))
                    .map(image -> resolveUrl(response.uri().toString(), image))
                    .orElse("");
            return new OgpMetadata(
                    title,
                    description,
                    imageUrl,
                    true
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
                    .header("User-Agent", "ReadStack/0.1")
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

    private String readLimited(InputStream inputStream) throws IOException {
        try (inputStream) {
            byte[] bytes = inputStream.readNBytes(MAX_BODY_BYTES + 1);
            if (bytes.length > MAX_BODY_BYTES) {
                throw new IOException("response body is too large");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private void closeQuietly(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException ignored) {
        }
    }

    private Optional<String> extractMeta(String html, String key) {
        Matcher matcher = META_TAG.matcher(html);
        while (matcher.find()) {
            String tag = matcher.group();
            String content = null;
            String property = null;
            String name = null;

            Matcher attributeMatcher = ATTRIBUTE.matcher(tag);
            while (attributeMatcher.find()) {
                String attributeName = attributeMatcher.group(1).toLowerCase();
                String attributeValue = clean(attributeMatcher.group(3));
                if ("content".equals(attributeName)) {
                    content = attributeValue;
                } else if ("property".equals(attributeName)) {
                    property = attributeValue;
                } else if ("name".equals(attributeName)) {
                    name = attributeValue;
                }
            }

            if (content != null && (key.equalsIgnoreCase(property) || key.equalsIgnoreCase(name))) {
                return Optional.of(content);
            }
        }
        return Optional.empty();
    }

    private Optional<String> extract(Pattern pattern, String html) {
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(clean(matcher.group(1)));
    }

    private String clean(String value) {
        return value == null ? "" : value
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String resolveUrl(String pageUrl, String imageUrl) {
        try {
            return URI.create(pageUrl).resolve(imageUrl).toString();
        } catch (IllegalArgumentException ignored) {
            return imageUrl;
        }
    }
}
