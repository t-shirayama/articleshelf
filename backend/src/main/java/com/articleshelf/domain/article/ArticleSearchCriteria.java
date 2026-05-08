package com.articleshelf.domain.article;

public record ArticleSearchCriteria(
        ArticleStatus status,
        String tag,
        String search,
        Boolean favorite
) {
}
