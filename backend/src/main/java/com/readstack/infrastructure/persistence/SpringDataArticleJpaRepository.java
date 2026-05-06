package com.readstack.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataArticleJpaRepository extends JpaRepository<ArticleEntity, UUID> {
    Optional<ArticleEntity> findByUrl(String url);

    boolean existsByUrl(String url);

    boolean existsByUrlAndIdNot(String url, UUID id);
}
