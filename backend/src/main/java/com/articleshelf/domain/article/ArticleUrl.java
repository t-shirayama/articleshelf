package com.articleshelf.domain.article;

public record ArticleUrl(String value) {
    public ArticleUrl {
        value = normalize(value);
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Article URL must not be blank");
        }
        return value.trim();
    }
}
