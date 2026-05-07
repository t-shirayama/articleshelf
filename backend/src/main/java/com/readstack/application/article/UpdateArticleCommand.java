package com.readstack.application.article;

import com.readstack.domain.article.ArticleStatus;

import java.time.LocalDate;
import java.util.List;

public record UpdateArticleCommand(
        String url,
        String title,
        String summary,
        ArticleStatus status,
        LocalDate readDate,
        Boolean favorite,
        Integer rating,
        String notes,
        List<String> tags
) {
}
