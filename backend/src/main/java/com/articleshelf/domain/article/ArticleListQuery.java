package com.articleshelf.domain.article;

import java.util.Locale;

public record ArticleListQuery(Integer page, Integer size, String sort) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;

    public boolean paged() {
        return page != null || size != null;
    }

    public int normalizedPage() {
        return Math.max(DEFAULT_PAGE, page == null ? DEFAULT_PAGE : page);
    }

    public int normalizedSize() {
        int requested = size == null ? DEFAULT_SIZE : size;
        return Math.max(1, Math.min(MAX_SIZE, requested));
    }

    public SortKey normalizedSort() {
        if (sort == null || sort.isBlank()) {
            return SortKey.CREATED_DESC;
        }

        String normalized = sort.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CREATED_ASC" -> SortKey.CREATED_ASC;
            case "UPDATED_DESC" -> SortKey.UPDATED_DESC;
            case "READ_DATE_DESC" -> SortKey.READ_DATE_DESC;
            case "TITLE_ASC" -> SortKey.TITLE_ASC;
            case "RATING_DESC" -> SortKey.RATING_DESC;
            case "CREATEDDESC", "CREATED_DESC" -> SortKey.CREATED_DESC;
            default -> SortKey.CREATED_DESC;
        };
    }

    public enum SortKey {
        CREATED_DESC,
        CREATED_ASC,
        UPDATED_DESC,
        READ_DATE_DESC,
        TITLE_ASC,
        RATING_DESC
    }
}
