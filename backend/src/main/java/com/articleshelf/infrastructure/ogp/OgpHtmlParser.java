package com.articleshelf.infrastructure.ogp;

import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class OgpHtmlParser {
    private static final Pattern META_TAG = Pattern.compile("<meta\\s+[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w:-]+)\\s*=\\s*(?:([\"'])(.*?)\\2|([^\\s\"'>/]+))", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CONTENT_TYPE_CHARSET = Pattern.compile("(?:^|;)\\s*charset\\s*=\\s*[\"']?([^;\"'\\s]+)", Pattern.CASE_INSENSITIVE);

    OgpMetadata parse(byte[] bodyBytes, String contentType, String pageUrl) {
        String body = decode(bodyBytes, contentType);
        String title = extractMeta(body, "og:title").or(() -> extract(HTML_TITLE, body)).orElse("");
        String description = extractMeta(body, "og:description")
                .or(() -> extractMeta(body, "description"))
                .orElse("");
        String imageUrl = extractMeta(body, "og:image")
                .or(() -> extractMeta(body, "twitter:image"))
                .map(image -> resolveUrl(pageUrl, image))
                .orElse("");
        return new OgpMetadata(title, description, imageUrl, true);
    }

    private String decode(byte[] bytes, String contentType) {
        Charset charset = charsetFromContentType(contentType)
                .or(() -> charsetFromMeta(new String(bytes, StandardCharsets.ISO_8859_1)))
                .orElse(StandardCharsets.UTF_8);
        return new String(bytes, charset);
    }

    private Optional<Charset> charsetFromContentType(String contentType) {
        Matcher matcher = CONTENT_TYPE_CHARSET.matcher(contentType == null ? "" : contentType);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return charsetByName(matcher.group(1));
    }

    private Optional<Charset> charsetFromMeta(String htmlPreview) {
        Matcher matcher = META_TAG.matcher(htmlPreview);
        while (matcher.find()) {
            String tag = matcher.group();
            Optional<Charset> charset = attributeValue(tag, "charset").flatMap(this::charsetByName);
            if (charset.isEmpty()) {
                charset = httpEquivContentCharset(tag);
            }
            if (charset.isPresent()) {
                return charset;
            }
        }
        return Optional.empty();
    }

    private Optional<Charset> httpEquivContentCharset(String tag) {
        Optional<String> httpEquiv = attributeValue(tag, "http-equiv");
        if (httpEquiv.isEmpty() || !"content-type".equalsIgnoreCase(httpEquiv.get())) {
            return Optional.empty();
        }
        return attributeValue(tag, "content").flatMap(this::charsetFromContentType);
    }

    private Optional<String> extractMeta(String html, String key) {
        Matcher matcher = META_TAG.matcher(html);
        while (matcher.find()) {
            String tag = matcher.group();
            String content = attributeValue(tag, "content").map(this::clean).orElse(null);
            String property = attributeValue(tag, "property").orElse(null);
            String name = attributeValue(tag, "name").orElse(null);

            if (content != null && (key.equalsIgnoreCase(property) || key.equalsIgnoreCase(name))) {
                return Optional.of(content);
            }
        }
        return Optional.empty();
    }

    private Optional<String> attributeValue(String tag, String key) {
        Matcher attributeMatcher = ATTRIBUTE.matcher(tag);
        while (attributeMatcher.find()) {
            String attributeName = attributeMatcher.group(1).toLowerCase(Locale.ROOT);
            if (!key.equals(attributeName)) {
                continue;
            }
            String quotedValue = attributeMatcher.group(3);
            String unquotedValue = attributeMatcher.group(4);
            return Optional.ofNullable(quotedValue != null ? quotedValue : unquotedValue);
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
        return value == null ? "" : HtmlUtils.htmlUnescape(value)
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

    private Optional<Charset> charsetByName(String name) {
        try {
            return Optional.of(Charset.forName(name.trim()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
