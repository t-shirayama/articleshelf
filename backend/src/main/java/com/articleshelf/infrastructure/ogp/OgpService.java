package com.articleshelf.infrastructure.ogp;

import com.articleshelf.application.article.ArticleMetadata;
import com.articleshelf.application.article.ArticleMetadataProvider;
import com.articleshelf.application.observability.BackendMetrics;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class OgpService implements ArticleMetadataProvider {
    private final OgpClient ogpClient;
    private final BackendMetrics metrics;

    public OgpService(OgpClient ogpClient, BackendMetrics metrics) {
        this.ogpClient = ogpClient;
        this.metrics = metrics;
    }

    @Override
    public ArticleMetadata fetch(String url) {
        if (url == null || url.isBlank()) {
            metrics.recordOgpFetch(Duration.ZERO, "invalid_input");
            return ArticleMetadata.unavailable();
        }
        long startedAt = System.nanoTime();
        try {
            OgpMetadata metadata = ogpClient.fetch(url);
            metrics.recordOgpFetch(
                    Duration.ofNanos(System.nanoTime() - startedAt),
                    metadata.accessible() ? "accessible" : "unavailable"
            );
            return new ArticleMetadata(
                    metadata.title(),
                    metadata.description(),
                    metadata.imageUrl(),
                    metadata.accessible()
            );
        } catch (RuntimeException exception) {
            metrics.recordOgpFetch(Duration.ofNanos(System.nanoTime() - startedAt), "error");
            throw exception;
        }
    }
}
