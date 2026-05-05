package com.readstack.application.article;

import com.readstack.domain.article.Article;
import com.readstack.domain.article.ArticleStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ArticleResponse(
        UUID id,
        String url,
        String title,
        String summary,
        ArticleStatus status,
        LocalDate readDate,
        boolean favorite,
        String notes,
        List<TagResponse> tags,
        Instant createdAt,
        Instant updatedAt
) {
    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
                article.getId(),
                article.getUrl(),
                article.getTitle(),
                article.getSummary(),
                article.getStatus(),
                article.getReadDate(),
                article.isFavorite(),
                article.getNotes(),
                article.getTags().stream()
                        .map(TagResponse::from)
                        .sorted(Comparator.comparing(TagResponse::name))
                        .toList(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }
}
