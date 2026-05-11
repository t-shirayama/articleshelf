package com.articleshelf.infrastructure.ogp;

import com.articleshelf.application.article.ArticleMetadata;
import com.articleshelf.application.observability.BackendMetrics;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OgpServiceTest {
    private final OgpClient client = mock(OgpClient.class);
    private final BackendMetrics metrics = mock(BackendMetrics.class);
    private final OgpService service = new OgpService(client, metrics);

    @Test
    void returnsUnavailableMetadataForBlankUrls() {
        ArticleMetadata metadata = service.fetch(" ");

        assertThat(metadata.accessible()).isFalse();
        verify(metrics).recordOgpFetch(Duration.ZERO, "invalid_input");
        verifyNoInteractions(client);
    }

    @Test
    void mapsAccessibleOgpMetadataAndRecordsMetrics() {
        when(client.fetch("https://example.com"))
                .thenReturn(new OgpMetadata("Title", "Description", "https://example.com/cover.png", true));

        ArticleMetadata metadata = service.fetch("https://example.com");

        assertThat(metadata.title()).isEqualTo("Title");
        assertThat(metadata.description()).isEqualTo("Description");
        assertThat(metadata.imageUrl()).isEqualTo("https://example.com/cover.png");
        assertThat(metadata.accessible()).isTrue();
        verify(metrics).recordOgpFetch(any(Duration.class), eq("accessible"));
    }

    @Test
    void recordsUnavailableAndErrorOutcomes() {
        when(client.fetch("https://example.com/unavailable")).thenReturn(OgpMetadata.unavailable());
        when(client.fetch("https://example.com/error")).thenThrow(new IllegalStateException("boom"));

        ArticleMetadata unavailable = service.fetch("https://example.com/unavailable");

        assertThat(unavailable.accessible()).isFalse();
        verify(metrics).recordOgpFetch(any(Duration.class), eq("unavailable"));
        assertThatThrownBy(() -> service.fetch("https://example.com/error"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        verify(metrics).recordOgpFetch(any(Duration.class), eq("error"));
    }
}
