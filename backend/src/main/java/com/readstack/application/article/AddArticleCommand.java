package com.readstack.application.article;

import com.readstack.domain.article.ArticleStatus;

import java.time.LocalDate;
import java.util.List;

public record AddArticleCommand(
        String url,
        String title,
        String summary,
        ArticleStatus status,
        LocalDate readDate,
        Boolean favorite,
        String notes,
        List<String> tags
) {
}
