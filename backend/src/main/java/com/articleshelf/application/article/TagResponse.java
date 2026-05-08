package com.articleshelf.application.article;

import com.articleshelf.domain.article.Tag;
import com.articleshelf.domain.article.TagUsage;

import java.time.Instant;
import java.util.UUID;

public record TagResponse(
        UUID id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        long articleCount
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getCreatedAt(), tag.getUpdatedAt(), 0);
    }

    public static TagResponse from(TagUsage tagUsage) {
        Tag tag = tagUsage.tag();
        return new TagResponse(tag.getId(), tag.getName(), tag.getCreatedAt(), tag.getUpdatedAt(), tagUsage.articleCount());
    }
}
