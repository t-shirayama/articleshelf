package com.articleshelf.domain.article;

import java.util.UUID;

public class ArticleVersionConflictException extends RuntimeException {
    private final UUID articleId;

    public ArticleVersionConflictException(UUID articleId) {
        super("article version conflict: " + articleId);
        this.articleId = articleId;
    }

    public UUID getArticleId() {
        return articleId;
    }
}
