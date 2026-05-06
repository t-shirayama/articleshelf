package com.readstack.infrastructure.persistence;

import com.readstack.domain.article.Article;
import com.readstack.domain.article.ArticleRepository;
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
    public List<Article> findAll() {
        return articleJpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Article> findById(UUID id) {
        return articleJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByUrl(String url) {
        return articleJpaRepository.existsByUrl(url);
    }

    @Override
    public boolean existsByUrlAndIdNot(String url, UUID id) {
        return articleJpaRepository.existsByUrlAndIdNot(url, id);
    }

    @Override
    public Article save(Article article) {
        ArticleEntity entity = articleJpaRepository.findById(article.getId()).orElseGet(ArticleEntity::new);
        entity.setId(article.getId());
        entity.setUrl(article.getUrl());
        entity.setTitle(article.getTitle());
        entity.setSummary(article.getSummary());
        entity.setThumbnailUrl(article.getThumbnailUrl());
        entity.setStatus(article.getStatus());
        entity.setReadDate(article.getReadDate());
        entity.setFavorite(article.isFavorite());
        entity.setRating(article.getRating());
        entity.setNotes(article.getNotes());
        entity.setTags(resolveTagEntities(article.getTags()));
        return toDomain(articleJpaRepository.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        articleJpaRepository.deleteById(id);
    }

    @Override
    public List<Tag> findAllTags() {
        return tagJpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Tag saveTag(String name) {
        String normalized = name == null ? "" : name.trim();
        TagEntity entity = tagJpaRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    TagEntity tag = new TagEntity();
                    tag.setName(normalized);
                    return tag;
                });
        return toDomain(tagJpaRepository.save(entity));
    }

    private Set<TagEntity> resolveTagEntities(Set<Tag> tags) {
        Set<TagEntity> entities = new LinkedHashSet<>();
        for (Tag tag : tags) {
            TagEntity entity = tagJpaRepository.findByNameIgnoreCase(tag.getName())
                    .orElseGet(() -> {
                        TagEntity newTag = new TagEntity();
                        newTag.setName(tag.getName());
                        return newTag;
                    });
            entities.add(tagJpaRepository.save(entity));
        }
        return entities;
    }

    private Article toDomain(ArticleEntity entity) {
        return new Article(
                entity.getId(),
                entity.getUrl(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getThumbnailUrl(),
                entity.getStatus(),
                entity.getReadDate(),
                entity.isFavorite(),
                entity.getRating() == null ? 0 : entity.getRating(),
                entity.getNotes(),
                entity.getTags().stream().map(this::toDomain).collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private Tag toDomain(TagEntity entity) {
        return new Tag(entity.getId(), entity.getName(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
