package com.articleshelf.application.article;

public interface ArticleMetadataProvider {
    ArticleMetadata fetch(String url);
}
