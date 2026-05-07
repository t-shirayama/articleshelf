package com.readstack.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface SpringDataArticleJpaRepository extends JpaRepository<ArticleEntity, UUID> {
    List<ArticleEntity> findAllByUserId(UUID userId);

    Optional<ArticleEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<ArticleEntity> findByUrlAndUserId(String url, UUID userId);

    boolean existsByUrlAndUserId(String url, UUID userId);

    boolean existsByUrlAndUserIdAndIdNot(String url, UUID userId, UUID id);

    List<ArticleEntity> findAllByUserIdIsNull();
}
