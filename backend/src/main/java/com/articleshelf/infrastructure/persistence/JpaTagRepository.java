package com.articleshelf.infrastructure.persistence;

import com.articleshelf.domain.article.Tag;
import com.articleshelf.domain.article.TagName;
import com.articleshelf.domain.article.TagNotFoundException;
import com.articleshelf.domain.article.TagRepository;
import com.articleshelf.domain.article.TagUsage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTagRepository implements TagRepository {
    private final SpringDataTagJpaRepository tagJpaRepository;
    private final SpringDataArticleTagJpaRepository articleTagJpaRepository;

    public JpaTagRepository(
            SpringDataTagJpaRepository tagJpaRepository,
            SpringDataArticleTagJpaRepository articleTagJpaRepository
    ) {
        this.tagJpaRepository = tagJpaRepository;
        this.articleTagJpaRepository = articleTagJpaRepository;
    }

    @Override
    public List<TagUsage> findAllTagUsagesByUserId(UUID userId) {
        return tagJpaRepository.findAllTagUsagesByUserId(userId).stream()
                .map(row -> new TagUsage(toDomain(row.tag()), row.articleCount()))
                .toList();
    }

    @Override
    public Tag saveTag(UUID userId, String name) {
        String normalized = TagName.normalize(name);
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
        return tagJpaRepository.findByUserIdAndNameIgnoreCase(userId, TagName.normalize(name)).map(this::toDomain);
    }

    @Override
    public long countArticlesByTagIdAndUserId(UUID tagId, UUID userId) {
        return articleTagJpaRepository.countByIdUserIdAndIdTagId(userId, tagId);
    }

    @Override
    public Tag renameTag(UUID userId, UUID tagId, String name) {
        TagEntity tag = tagJpaRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new TagNotFoundException(tagId));
        tag.setName(TagName.normalize(name));
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

    private Tag toDomain(TagEntity entity) {
        return new Tag(entity.getId(), entity.getUserId(), entity.getName(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
