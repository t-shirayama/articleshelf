package com.articleshelf.infrastructure.persistence;

import com.articleshelf.application.article.ArticleListQuery;
import com.articleshelf.domain.article.Article;
import com.articleshelf.domain.article.ArticleRepository;
import com.articleshelf.domain.article.ArticleSearchCriteria;
import com.articleshelf.domain.article.ArticleVersionConflictException;
import com.articleshelf.domain.article.Tag;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.time.LocalDate;
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
    public List<Article> searchByUserId(UUID userId, ArticleSearchCriteria criteria, ArticleListQuery query) {
        if (query.paged()) {
            Pageable pageable = PageRequest.of(query.normalizedPage(), query.normalizedSize(), toSort(query));
            return articleJpaRepository.searchByUserId(
                    userId,
                    criteria.status(),
                    hasTags(criteria),
                    parameterizedTags(criteria),
                    toLikePattern(criteria.search()),
                    criteria.favorite(),
                    hasRatings(criteria),
                    parameterizedRatings(criteria),
                    hasCreatedFrom(criteria),
                    parameterizedCreatedFrom(criteria),
                    hasCreatedTo(criteria),
                    parameterizedCreatedTo(criteria),
                    hasReadFrom(criteria),
                    parameterizedReadFrom(criteria),
                    hasReadTo(criteria),
                    parameterizedReadTo(criteria),
                    pageable
            ).stream().map(this::toDomain).toList();
        }

        return articleJpaRepository.searchByUserId(
                userId,
                criteria.status(),
                hasTags(criteria),
                parameterizedTags(criteria),
                toLikePattern(criteria.search()),
                criteria.favorite(),
                hasRatings(criteria),
                parameterizedRatings(criteria),
                hasCreatedFrom(criteria),
                parameterizedCreatedFrom(criteria),
                hasCreatedTo(criteria),
                parameterizedCreatedTo(criteria),
                hasReadFrom(criteria),
                parameterizedReadFrom(criteria),
                hasReadTo(criteria),
                parameterizedReadTo(criteria),
                toSort(query)
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
        if (entity.getId() != null && !article.getId().equals(entity.getId())) {
            throw new IllegalStateException("loaded wrong article entity");
        }
        if (entity.getVersion() != null && entity.getVersion() != article.getVersion()) {
            throw new ArticleVersionConflictException(article.getId());
        }
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
        try {
            return toDomain(articleJpaRepository.saveAndFlush(entity));
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ArticleVersionConflictException(article.getId());
        }
    }

    @Override
    public void deleteByIdAndUserId(UUID id, UUID userId) {
        articleJpaRepository.findByIdAndUserId(id, userId).ifPresent(articleJpaRepository::delete);
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
                entity.getVersion() == null ? 0L : entity.getVersion(),
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

    private Sort toSort(ArticleListQuery query) {
        return switch (query.normalizedSort()) {
            case CREATED_ASC -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.desc("id"));
            case UPDATED_DESC -> Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case READ_DATE_DESC -> Sort.by(
                    Sort.Order.desc("readDate").nullsLast(),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case TITLE_ASC -> Sort.by(Sort.Order.asc("title"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case RATING_DESC -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case CREATED_DESC -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        };
    }

    private boolean hasTags(ArticleSearchCriteria criteria) {
        return criteria.tags() != null && !criteria.tags().isEmpty();
    }

    private List<String> parameterizedTags(ArticleSearchCriteria criteria) {
        return hasTags(criteria) ? criteria.tags() : List.of("__unused_tag__");
    }

    private boolean hasRatings(ArticleSearchCriteria criteria) {
        return criteria.ratings() != null && !criteria.ratings().isEmpty();
    }

    private List<Integer> parameterizedRatings(ArticleSearchCriteria criteria) {
        return hasRatings(criteria) ? criteria.ratings() : List.of(-1);
    }

    private boolean hasCreatedFrom(ArticleSearchCriteria criteria) {
        return criteria.createdFrom() != null;
    }

    private Instant parameterizedCreatedFrom(ArticleSearchCriteria criteria) {
        return hasCreatedFrom(criteria) ? criteria.createdFrom() : Instant.EPOCH;
    }

    private boolean hasCreatedTo(ArticleSearchCriteria criteria) {
        return criteria.createdToExclusive() != null;
    }

    private Instant parameterizedCreatedTo(ArticleSearchCriteria criteria) {
        return hasCreatedTo(criteria) ? criteria.createdToExclusive() : Instant.EPOCH;
    }

    private boolean hasReadFrom(ArticleSearchCriteria criteria) {
        return criteria.readFrom() != null;
    }

    private LocalDate parameterizedReadFrom(ArticleSearchCriteria criteria) {
        return hasReadFrom(criteria) ? criteria.readFrom() : LocalDate.of(1970, 1, 1);
    }

    private boolean hasReadTo(ArticleSearchCriteria criteria) {
        return criteria.readTo() != null;
    }

    private LocalDate parameterizedReadTo(ArticleSearchCriteria criteria) {
        return hasReadTo(criteria) ? criteria.readTo() : LocalDate.of(1970, 1, 1);
    }
}
