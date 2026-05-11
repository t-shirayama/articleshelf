package com.articleshelf.application.article;

public record ArticlePreviewResponse(
        String url,
        String title,
        String summary,
        String thumbnailUrl,
        boolean previewAvailable,
        String errorReason
) {
    public static ArticlePreviewResponse from(String url, ArticleMetadata metadata) {
        if (!metadata.accessible()) {
            return unavailable(url);
        }
        return new ArticlePreviewResponse(
                url,
                valueOrEmpty(metadata.title()),
                valueOrEmpty(metadata.description()),
                valueOrEmpty(metadata.imageUrl()),
                true,
                null
        );
    }

    private static ArticlePreviewResponse unavailable(String url) {
        return new ArticlePreviewResponse(url, "", "", "", false, "OGP_FETCH_FAILED");
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
