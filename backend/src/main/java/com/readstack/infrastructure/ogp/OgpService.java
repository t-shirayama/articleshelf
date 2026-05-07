package com.readstack.infrastructure.ogp;

import com.readstack.application.article.ArticleMetadata;
import com.readstack.application.article.ArticleMetadataProvider;
import org.springframework.stereotype.Service;

@Service
public class OgpService implements ArticleMetadataProvider {
    private final OgpClient ogpClient;

    public OgpService(OgpClient ogpClient) {
        this.ogpClient = ogpClient;
    }

    @Override
    public ArticleMetadata fetch(String url) {
        if (url == null || url.isBlank()) {
            return ArticleMetadata.unavailable();
        }
        OgpMetadata metadata = ogpClient.fetch(url);
        return new ArticleMetadata(
                metadata.title(),
                metadata.description(),
                metadata.imageUrl(),
                metadata.accessible()
        );
    }
}
