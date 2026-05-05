package com.readstack.domain.article;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository {
    List<Article> findAll();

    Optional<Article> findById(UUID id);

    boolean existsByUrl(String url);

    boolean existsByUrlAndIdNot(String url, UUID id);

    Article save(Article article);

    void deleteById(UUID id);

    List<Tag> findAllTags();

    Tag saveTag(String name);
}
