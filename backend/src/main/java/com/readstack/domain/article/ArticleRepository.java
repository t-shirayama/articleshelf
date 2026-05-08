package com.readstack.domain.article;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository {
    List<Article> findAllByUserId(UUID userId);

    List<Article> searchByUserId(UUID userId, ArticleSearchCriteria criteria);

    Optional<Article> findByIdAndUserId(UUID id, UUID userId);

    Optional<Article> findByUrlAndUserId(String url, UUID userId);

    Article save(Article article);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
