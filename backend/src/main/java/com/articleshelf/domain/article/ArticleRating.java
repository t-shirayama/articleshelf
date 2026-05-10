package com.articleshelf.domain.article;

public record ArticleRating(int value) {
    public static final int MIN = 0;
    public static final int MAX = 5;

    public ArticleRating {
        value = normalize(value);
    }

    public static int normalize(Integer value) {
        return normalize(value == null ? MIN : value.intValue());
    }

    public static int normalize(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }
}
