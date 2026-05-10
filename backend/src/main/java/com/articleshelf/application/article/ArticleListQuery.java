package com.articleshelf.application.article;

import java.util.List;

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

    public <T> List<T> slice(List<T> items) {
        if (!paged()) {
            return items;
        }
        int fromIndex = Math.min(items.size(), normalizedPage() * normalizedSize());
        int toIndex = Math.min(items.size(), fromIndex + normalizedSize());
        return items.subList(fromIndex, toIndex);
    }
}
