package com.readstack.domain.article;

public class DuplicateArticleUrlException extends RuntimeException {
    public DuplicateArticleUrlException(String url) {
        super("Article URL already exists: " + url);
    }
}
