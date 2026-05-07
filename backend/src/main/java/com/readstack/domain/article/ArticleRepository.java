package com.readstack.domain.article;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository {
    List<Article> findAllByUserId(UUID userId);

    List<Article> searchByUserId(UUID userId, ArticleSearchCriteria criteria);

    Optional<Article> findByIdAndUserId(UUID id, UUID userId);

    Optional<Article> findByUrlAndUserId(String url, UUID userId);

    boolean existsByUrlAndUserId(String url, UUID userId);

    boolean existsByUrlAndUserIdAndIdNot(String url, UUID userId, UUID id);

    Article save(Article article);

    void deleteByIdAndUserId(UUID id, UUID userId);

    List<TagUsage> findAllTagUsagesByUserId(UUID userId);

    Tag saveTag(UUID userId, String name);

    Optional<Tag> findTagByIdAndUserId(UUID id, UUID userId);

    Optional<Tag> findTagByNameAndUserId(String name, UUID userId);

    long countArticlesByTagIdAndUserId(UUID tagId, UUID userId);

    Tag renameTag(UUID userId, UUID tagId, String name);

    void mergeTags(UUID userId, UUID sourceTagId, UUID targetTagId);

    void deleteTagByIdAndUserId(UUID tagId, UUID userId);
}
