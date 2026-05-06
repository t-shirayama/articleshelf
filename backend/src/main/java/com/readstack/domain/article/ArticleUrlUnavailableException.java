package com.readstack.domain.article;

public class ArticleUrlUnavailableException extends RuntimeException {
    public ArticleUrlUnavailableException(String url) {
        super("このURLにアクセスできませんでした。URLが正しいか、ページが公開されているか確認してください");
    }
}
