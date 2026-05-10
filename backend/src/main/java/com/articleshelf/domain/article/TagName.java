package com.articleshelf.domain.article;

public record TagName(String value) {
    public static final int MAX_LENGTH = 255;

    public TagName {
        value = normalize(value);
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Tag name must be 255 characters or fewer");
        }
        return normalized;
    }
}
