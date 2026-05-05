package com.readstack.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataArticleJpaRepository extends JpaRepository<ArticleEntity, UUID> {
    boolean existsByUrl(String url);

    boolean existsByUrlAndIdNot(String url, UUID id);
}
