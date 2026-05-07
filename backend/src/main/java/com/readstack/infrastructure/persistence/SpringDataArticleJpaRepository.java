package com.readstack.infrastructure.persistence;

import com.readstack.domain.article.ArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataArticleJpaRepository extends JpaRepository<ArticleEntity, UUID> {
    List<ArticleEntity> findAllByUserId(UUID userId);

    @Query("""
            select distinct article
            from ArticleEntity article
            left join article.articleTags articleTag
            left join articleTag.tag tag
            where article.userId = :userId
              and (:status is null or article.status = :status)
              and (:favorite is null or article.favorite = :favorite)
              and (:tag is null or lower(tag.name) = :tag)
              and (
                    :searchPattern is null
                    or lower(article.title) like :searchPattern
                    or lower(article.url) like :searchPattern
                    or lower(coalesce(article.summary, '')) like :searchPattern
                    or lower(coalesce(article.notes, '')) like :searchPattern
              )
            order by article.createdAt desc
            """)
    List<ArticleEntity> searchByUserId(
            @Param("userId") UUID userId,
            @Param("status") ArticleStatus status,
            @Param("tag") String tag,
            @Param("searchPattern") String searchPattern,
            @Param("favorite") Boolean favorite
    );

    Optional<ArticleEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<ArticleEntity> findByUrlAndUserId(String url, UUID userId);

    boolean existsByUrlAndUserId(String url, UUID userId);

    boolean existsByUrlAndUserIdAndIdNot(String url, UUID userId, UUID id);

    List<ArticleEntity> findAllByUserIdIsNull();
}
