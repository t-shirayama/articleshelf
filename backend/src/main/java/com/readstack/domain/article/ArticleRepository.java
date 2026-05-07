package com.readstack.domain.article;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository {
    List<Article> findAllByUserId(UUID userId);

    Optional<Article> findByIdAndUserId(UUID id, UUID userId);

    Optional<Article> findByUrlAndUserId(String url, UUID userId);

    boolean existsByUrlAndUserId(String url, UUID userId);

    boolean existsByUrlAndUserIdAndIdNot(String url, UUID userId, UUID id);

    Article save(Article article);

    void deleteByIdAndUserId(UUID id, UUID userId);

    List<Tag> findAllTagsByUserId(UUID userId);

    Tag saveTag(UUID userId, String name);
}
