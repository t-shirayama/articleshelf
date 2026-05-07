package com.readstack.application.article;

public interface ArticleMetadataProvider {
    ArticleMetadata fetch(String url);
}
