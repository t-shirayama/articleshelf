package com.readstack.infrastructure.persistence;

import com.readstack.domain.article.Article;
import com.readstack.domain.article.ArticleRepository;
import com.readstack.domain.article.ArticleSearchCriteria;
import com.readstack.domain.article.Tag;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class JpaArticleRepository implements ArticleRepository {
    private final SpringDataArticleJpaRepository articleJpaRepository;
    private final SpringDataTagJpaRepository tagJpaRepository;

    public JpaArticleRepository(
            SpringDataArticleJpaRepository articleJpaRepository,
            SpringDataTagJpaRepository tagJpaRepository
    ) {
        this.articleJpaRepository = articleJpaRepository;
        this.tagJpaRepository = tagJpaRepository;
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
                criteria.search(),
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
    public boolean existsByUrlAndUserId(String url, UUID userId) {
        return articleJpaRepository.existsByUrlAndUserId(url, userId);
    }

    @Override
    public boolean existsByUrlAndUserIdAndIdNot(String url, UUID userId, UUID id) {
        return articleJpaRepository.existsByUrlAndUserIdAndIdNot(url, userId, id);
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
    public List<Tag> findAllTagsByUserId(UUID userId) {
        return tagJpaRepository.findAllByUserId(userId).stream().map(this::toDomain).toList();
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
}
