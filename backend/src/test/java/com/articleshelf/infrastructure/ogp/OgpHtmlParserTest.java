package com.articleshelf.infrastructure.ogp;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OgpHtmlParserTest {
    private final OgpHtmlParser parser = new OgpHtmlParser();

    @Test
    void extractsOgMetadataWithUnquotedAndReorderedAttributes() {
        String html = """
                <html>
                  <head>
                    <meta content="Article &amp; Memo" property=og:title>
                    <meta name='description' content=' Short   description '>
                    <meta content="/images/card.png" property="twitter:image">
                  </head>
                </html>
                """;

        OgpMetadata metadata = parser.parse(html.getBytes(StandardCharsets.UTF_8), "text/html; charset=utf-8", "https://example.com/articles/1");

        assertThat(metadata.title()).isEqualTo("Article & Memo");
        assertThat(metadata.description()).isEqualTo("Short description");
        assertThat(metadata.imageUrl()).isEqualTo("https://example.com/images/card.png");
        assertThat(metadata.accessible()).isTrue();
    }

    @Test
    void usesContentTypeCharsetBeforeDecodingBody() {
        String html = """
                <html><head><meta property="og:title" content="技術記事"></head></html>
                """;

        OgpMetadata metadata = parser.parse(html.getBytes(Charset.forName("Shift_JIS")), "text/html; charset=Shift_JIS", "https://example.com");

        assertThat(metadata.title()).isEqualTo("技術記事");
    }

    @Test
    void usesMetaCharsetWhenContentTypeOmitsCharset() {
        String html = """
                <html><head><meta charset="Shift_JIS"><title>日本語タイトル</title></head></html>
                """;

        OgpMetadata metadata = parser.parse(html.getBytes(Charset.forName("Shift_JIS")), "text/html", "https://example.com");

        assertThat(metadata.title()).isEqualTo("日本語タイトル");
    }
}
