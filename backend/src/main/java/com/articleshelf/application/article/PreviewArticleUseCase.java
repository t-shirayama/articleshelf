package com.articleshelf.application.article;

import com.articleshelf.application.auth.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class PreviewArticleUseCase {
    private final ArticleMetadataProvider metadataProvider;
    private final ArticleUrlUniquenessGuard urlUniquenessGuard;

    public PreviewArticleUseCase(
            ArticleMetadataProvider metadataProvider,
            ArticleUrlUniquenessGuard urlUniquenessGuard
    ) {
        this.metadataProvider = metadataProvider;
        this.urlUniquenessGuard = urlUniquenessGuard;
    }

    public ArticlePreviewResponse preview(CurrentUser user, String url) {
        String normalizedUrl = url.trim();
        urlUniquenessGuard.validate(user, normalizedUrl, null);
        return ArticlePreviewResponse.from(normalizedUrl, metadataProvider.fetch(normalizedUrl));
    }
}
