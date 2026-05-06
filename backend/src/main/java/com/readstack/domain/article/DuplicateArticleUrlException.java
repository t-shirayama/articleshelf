package com.readstack.domain.article;

public class DuplicateArticleUrlException extends RuntimeException {
    public DuplicateArticleUrlException(String url) {
        super("このURLはすでに登録されています");
    }
}
