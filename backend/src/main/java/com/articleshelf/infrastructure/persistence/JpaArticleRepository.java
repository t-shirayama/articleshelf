package com.articleshelf.infrastructure.persistence;

import com.articleshelf.domain.article.Article;
import com.articleshelf.domain.article.ArticleRepository;
import com.articleshelf.domain.article.ArticleSearchCriteria;
import com.articleshelf.domain.article.Tag;
import com.articleshelf.domain.article.TagNotFoundException;
import com.articleshelf.domain.article.TagRepository;
import com.articleshelf.domain.article.TagUsage;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class JpaArticleRepository implements ArticleRepository, TagRepository {
    private final SpringDataArticleJpaRepository articleJpaRepository;
    private final SpringDataTagJpaRepository tagJpaRepository;
    private final SpringDataArticleTagJpaRepository articleTagJpaRepository;

    public JpaArticleRepository(
            SpringDataArticleJpaRepository articleJpaRepository,
            SpringDataTagJpaRepository tagJpaRepository,
            SpringDataArticleTagJpaRepository articleTagJpaRepository
    ) {
        this.articleJpaRepository = articleJpaRepository;
        this.tagJpaRepository = tagJpaRepository;
        this.articleTagJpaRepository = articleTagJpaRepository;
    }

    @Override
    public List<Article> findAllByUserId(UUID userId) {
        return articleJpaRepository.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Article> searchByUserId(UUID userId, ArticleSearchCriteria criteria) {
        return articleJpaRepository.searchByUserId(
                userId,
                criteria.status(),
                criteria.tag(),
                toLikePattern(criteria.search()),
                criteria.favorite()
        ).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Article> findByIdAndUserId(UUID id, UUID userId) {
        return articleJpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public Optional<Article> findByUrlAndUserId(String url, UUID userId) {
        return articleJpaRepository.findByUrlAndUserId(url, userId).map(this::toDomain);
    }

    @Override
    public Article save(Article article) {
        ArticleEntity entity = articleJpaRepository.findByIdAndUserId(article.getId(), article.getUserId())
                .orElseGet(ArticleEntity::new);
        entity.setId(article.getId());
        entity.setUserId(article.getUserId());
        entity.setUrl(article.getUrl());
        entity.setTitle(article.getTitle());
        entity.setSummary(article.getSummary());
        entity.setThumbnailUrl(article.getThumbnailUrl());
        entity.setStatus(article.getStatus());
        entity.setReadDate(article.getReadDate());
        entity.setFavorite(article.isFavorite());
        entity.setRating(article.getRating());
        entity.setNotes(article.getNotes());
        entity.setArticleTags(resolveArticleTagEntities(entity, article.getUserId(), article.getTags()));
        return toDomain(articleJpaRepository.save(entity));
    }

    @Override
    public void deleteByIdAndUserId(UUID id, UUID userId) {
        articleJpaRepository.findByIdAndUserId(id, userId).ifPresent(articleJpaRepository::delete);
    }

    @Override
    public List<TagUsage> findAllTagUsagesByUserId(UUID userId) {
        return tagJpaRepository.findAllTagUsagesByUserId(userId).stream()
                .map(row -> new TagUsage(toDomain(row.tag()), row.articleCount()))
                .toList();
    }

    @Override
    public Tag saveTag(UUID userId, String name) {
        String normalized = name == null ? "" : name.trim();
        TagEntity entity = tagJpaRepository.findByUserIdAndNameIgnoreCase(userId, normalized)
                .orElseGet(() -> {
                    TagEntity tag = new TagEntity();
                    tag.setUserId(userId);
                    tag.setName(normalized);
                    return tag;
                });
        return toDomain(tagJpaRepository.save(entity));
    }

    @Override
    public Optional<Tag> findTagByIdAndUserId(UUID id, UUID userId) {
        return tagJpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public Optional<Tag> findTagByNameAndUserId(String name, UUID userId) {
        return tagJpaRepository.findByUserIdAndNameIgnoreCase(userId, name).map(this::toDomain);
    }

    @Override
    public long countArticlesByTagIdAndUserId(UUID tagId, UUID userId) {
        return articleTagJpaRepository.countByIdUserIdAndIdTagId(userId, tagId);
    }

    @Override
    public Tag renameTag(UUID userId, UUID tagId, String name) {
        TagEntity tag = tagJpaRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new TagNotFoundException(tagId));
        tag.setName(name == null ? "" : name.trim());
        return toDomain(tagJpaRepository.save(tag));
    }

    @Override
    public void mergeTags(UUID userId, UUID sourceTagId, UUID targetTagId) {
        tagJpaRepository.findByIdAndUserId(targetTagId, userId)
                .orElseThrow(() -> new TagNotFoundException(targetTagId));
        TagEntity sourceTag = tagJpaRepository.findByIdAndUserId(sourceTagId, userId)
                .orElseThrow(() -> new TagNotFoundException(sourceTagId));
        articleTagJpaRepository.copyMissingLinksToTag(userId, sourceTagId, targetTagId);
        articleTagJpaRepository.deleteAllByUserIdAndTagId(userId, sourceTagId);
        articleTagJpaRepository.flush();
        tagJpaRepository.delete(sourceTag);
    }

    @Override
    public void deleteTagByIdAndUserId(UUID tagId, UUID userId) {
        tagJpaRepository.findByIdAndUserId(tagId, userId).ifPresent(tagJpaRepository::delete);
    }

    private Set<ArticleTagEntity> resolveArticleTagEntities(ArticleEntity articleEntity, UUID userId, Set<Tag> tags) {
        Set<ArticleTagEntity> entities = new LinkedHashSet<>();
        for (Tag tag : tags) {
            if (!userId.equals(tag.getUserId())) {
                throw new IllegalStateException("article and tag user mismatch");
            }
            TagEntity entity = tagJpaRepository.findByUserIdAndNameIgnoreCase(userId, tag.getName())
                    .orElseGet(() -> {
                        TagEntity newTag = new TagEntity();
                        newTag.setUserId(userId);
                        newTag.setName(tag.getName());
                        return newTag;
                    });
            entities.add(ArticleTagEntity.link(articleEntity, tagJpaRepository.save(entity)));
        }
        return entities;
    }

    private Article toDomain(ArticleEntity entity) {
        return new Article(
                entity.getId(),
                entity.getUserId(),
                entity.getUrl(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getThumbnailUrl(),
                entity.getStatus(),
                entity.getReadDate(),
                entity.isFavorite(),
                entity.getRating() == null ? 0 : entity.getRating(),
                entity.getNotes(),
                entity.getArticleTags().stream()
                        .map(ArticleTagEntity::getTag)
                        .map(this::toDomain)
                        .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private Tag toDomain(TagEntity entity) {
        return new Tag(entity.getId(), entity.getUserId(), entity.getName(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private String toLikePattern(String value) {
        return value == null ? null : "%" + escapeLikePattern(value) + "%";
    }

    private String escapeLikePattern(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
