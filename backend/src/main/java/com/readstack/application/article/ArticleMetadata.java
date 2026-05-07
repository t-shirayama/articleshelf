package com.readstack.application.article;

public record ArticleMetadata(String title, String description, String imageUrl, boolean accessible) {
    public static ArticleMetadata empty() {
        return new ArticleMetadata("", "", "", true);
    }

    public static ArticleMetadata unavailable() {
        return new ArticleMetadata("", "", "", false);
    }
}
