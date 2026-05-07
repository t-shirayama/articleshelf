package com.readstack.domain.article;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository {
    List<TagUsage> findAllTagUsagesByUserId(UUID userId);

    Tag saveTag(UUID userId, String name);

    Optional<Tag> findTagByIdAndUserId(UUID id, UUID userId);

    Optional<Tag> findTagByNameAndUserId(String name, UUID userId);

    long countArticlesByTagIdAndUserId(UUID tagId, UUID userId);

    Tag renameTag(UUID userId, UUID tagId, String name);

    void mergeTags(UUID userId, UUID sourceTagId, UUID targetTagId);

    void deleteTagByIdAndUserId(UUID tagId, UUID userId);
}
