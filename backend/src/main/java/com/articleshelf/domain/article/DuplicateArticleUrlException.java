package com.articleshelf.domain.article;

import java.util.UUID;

public class DuplicateArticleUrlException extends RuntimeException {
    private final UUID existingArticleId;

    public DuplicateArticleUrlException(String url, UUID existingArticleId) {
        super("このURLはすでに登録されています");
        this.existingArticleId = existingArticleId;
    }

    public UUID getExistingArticleId() {
        return existingArticleId;
    }
}
