package com.readstack.infrastructure.ogp;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OgpClient {
    private static final Pattern META_TAG = Pattern.compile("<meta\\s+[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w:-]+)\\s*=\\s*([\"'])(.*?)\\2", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public OgpMetadata fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "ReadStack/0.1")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return OgpMetadata.unavailable();
            }
            String body = response.body();
            String title = extractMeta(body, "og:title").or(() -> extract(HTML_TITLE, body)).orElse("");
            String description = extractMeta(body, "og:description")
                    .or(() -> extractMeta(body, "description"))
                    .orElse("");
            String imageUrl = extractMeta(body, "og:image")
                    .or(() -> extractMeta(body, "twitter:image"))
                    .map(image -> resolveUrl(url, image))
                    .orElse("");
            return new OgpMetadata(
                    title,
                    description,
                    imageUrl,
                    true
            );
        } catch (IllegalArgumentException ignored) {
            return OgpMetadata.unavailable();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return OgpMetadata.unavailable();
        } catch (IOException | UncheckedIOException ignored) {
            return OgpMetadata.unavailable();
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
