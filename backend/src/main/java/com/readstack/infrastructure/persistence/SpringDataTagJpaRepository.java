package com.readstack.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataTagJpaRepository extends JpaRepository<TagEntity, UUID> {
    List<TagEntity> findAllByUserId(UUID userId);

    @Query("""
            select new com.readstack.infrastructure.persistence.TagUsageRow(tag, count(articleTag.id.articleId))
            from TagEntity tag
            left join ArticleTagEntity articleTag
              on articleTag.id.userId = tag.userId
             and articleTag.id.tagId = tag.id
            where tag.userId = :userId
            group by tag
            """)
    List<TagUsageRow> findAllTagUsagesByUserId(@Param("userId") UUID userId);

    Optional<TagEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<TagEntity> findByUserIdAndNameIgnoreCase(UUID userId, String name);

    List<TagEntity> findAllByUserIdIsNull();
}
