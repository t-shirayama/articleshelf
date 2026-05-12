package com.articleshelf.domain.article;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ArticleSearchCriteria(
        ArticleStatus status,
        List<String> tags,
        String search,
        Boolean favorite,
        List<Integer> ratings,
        Instant createdFrom,
        Instant createdToExclusive,
        LocalDate readFrom,
        LocalDate readTo
) {
    public ArticleSearchCriteria(ArticleStatus status, String tag, String search, Boolean favorite) {
        this(
                status,
                tag == null || tag.isBlank() ? List.of() : List.of(tag),
                search,
                favorite,
                List.of(),
                null,
                null,
                null,
                null
        );
    }
}
