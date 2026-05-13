package com.articleshelf.infrastructure.persistence;

import com.articleshelf.domain.article.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
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
              and (:tagsEnabled = false or lower(tag.name) in :tags)
              and (:ratingsEnabled = false or article.rating in :ratings)
              and (:createdFromEnabled = false or article.createdAt >= :createdFrom)
              and (:createdToEnabled = false or article.createdAt < :createdToExclusive)
              and (:readFromEnabled = false or article.readDate >= :readFrom)
              and (:readToEnabled = false or article.readDate <= :readTo)
              and (
                    :searchPattern is null
                    or lower(article.title) like :searchPattern escape '!'
                    or lower(article.url) like :searchPattern escape '!'
                    or lower(coalesce(article.summary, '')) like :searchPattern escape '!'
                    or lower(coalesce(article.notes, '')) like :searchPattern escape '!'
                    or lower(coalesce(tag.name, '')) like :searchPattern escape '!'
              )
            """)
    List<ArticleEntity> searchByUserId(
            @Param("userId") UUID userId,
            @Param("status") ArticleStatus status,
            @Param("tagsEnabled") boolean tagsEnabled,
            @Param("tags") List<String> tags,
            @Param("searchPattern") String searchPattern,
            @Param("favorite") Boolean favorite,
            @Param("ratingsEnabled") boolean ratingsEnabled,
            @Param("ratings") List<Integer> ratings,
            @Param("createdFromEnabled") boolean createdFromEnabled,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdToEnabled") boolean createdToEnabled,
            @Param("createdToExclusive") Instant createdToExclusive,
            @Param("readFromEnabled") boolean readFromEnabled,
            @Param("readFrom") LocalDate readFrom,
            @Param("readToEnabled") boolean readToEnabled,
            @Param("readTo") LocalDate readTo,
            Sort sort
    );

    @Query(value = """
            select distinct article
            from ArticleEntity article
            left join article.articleTags articleTag
            left join articleTag.tag tag
            where article.userId = :userId
              and (:status is null or article.status = :status)
              and (:favorite is null or article.favorite = :favorite)
              and (:tagsEnabled = false or lower(tag.name) in :tags)
              and (:ratingsEnabled = false or article.rating in :ratings)
              and (:createdFromEnabled = false or article.createdAt >= :createdFrom)
              and (:createdToEnabled = false or article.createdAt < :createdToExclusive)
              and (:readFromEnabled = false or article.readDate >= :readFrom)
              and (:readToEnabled = false or article.readDate <= :readTo)
              and (
                    :searchPattern is null
                    or lower(article.title) like :searchPattern escape '!'
                    or lower(article.url) like :searchPattern escape '!'
                    or lower(coalesce(article.summary, '')) like :searchPattern escape '!'
                    or lower(coalesce(article.notes, '')) like :searchPattern escape '!'
                    or lower(coalesce(tag.name, '')) like :searchPattern escape '!'
              )
            """,
            countQuery = """
            select count(distinct article.id)
            from ArticleEntity article
            left join article.articleTags articleTag
            left join articleTag.tag tag
            where article.userId = :userId
              and (:status is null or article.status = :status)
              and (:favorite is null or article.favorite = :favorite)
              and (:tagsEnabled = false or lower(tag.name) in :tags)
              and (:ratingsEnabled = false or article.rating in :ratings)
              and (:createdFromEnabled = false or article.createdAt >= :createdFrom)
              and (:createdToEnabled = false or article.createdAt < :createdToExclusive)
              and (:readFromEnabled = false or article.readDate >= :readFrom)
              and (:readToEnabled = false or article.readDate <= :readTo)
              and (
                    :searchPattern is null
                    or lower(article.title) like :searchPattern escape '!'
                    or lower(article.url) like :searchPattern escape '!'
                    or lower(coalesce(article.summary, '')) like :searchPattern escape '!'
                    or lower(coalesce(article.notes, '')) like :searchPattern escape '!'
                    or lower(coalesce(tag.name, '')) like :searchPattern escape '!'
              )
            """)
    Page<ArticleEntity> searchByUserId(
            @Param("userId") UUID userId,
            @Param("status") ArticleStatus status,
            @Param("tagsEnabled") boolean tagsEnabled,
            @Param("tags") List<String> tags,
            @Param("searchPattern") String searchPattern,
            @Param("favorite") Boolean favorite,
            @Param("ratingsEnabled") boolean ratingsEnabled,
            @Param("ratings") List<Integer> ratings,
            @Param("createdFromEnabled") boolean createdFromEnabled,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdToEnabled") boolean createdToEnabled,
            @Param("createdToExclusive") Instant createdToExclusive,
            @Param("readFromEnabled") boolean readFromEnabled,
            @Param("readFrom") LocalDate readFrom,
            @Param("readToEnabled") boolean readToEnabled,
            @Param("readTo") LocalDate readTo,
            Pageable pageable
    );

    Optional<ArticleEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<ArticleEntity> findByUrlAndUserId(String url, UUID userId);

    List<ArticleEntity> findAllByUserIdIsNull();
}
