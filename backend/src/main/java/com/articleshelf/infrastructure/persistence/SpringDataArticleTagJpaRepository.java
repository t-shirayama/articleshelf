package com.articleshelf.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SpringDataArticleTagJpaRepository extends JpaRepository<ArticleTagEntity, ArticleTagId> {
    long countByIdUserIdAndIdTagId(UUID userId, UUID tagId);

    @Modifying
    @Query(value = """
            insert into article_tags (user_id, article_id, tag_id)
            select source.user_id, source.article_id, :targetTagId
            from article_tags source
            where source.user_id = :userId
              and source.tag_id = :sourceTagId
              and not exists (
                    select 1
                    from article_tags existing
                    where existing.user_id = source.user_id
                      and existing.article_id = source.article_id
                      and existing.tag_id = :targetTagId
              )
            """, nativeQuery = true)
    int copyMissingLinksToTag(
            @Param("userId") UUID userId,
            @Param("sourceTagId") UUID sourceTagId,
            @Param("targetTagId") UUID targetTagId
    );

    @Modifying
    @Query("""
            delete from ArticleTagEntity articleTag
            where articleTag.id.userId = :userId
              and articleTag.id.tagId = :tagId
            """)
    int deleteAllByUserIdAndTagId(@Param("userId") UUID userId, @Param("tagId") UUID tagId);
}
