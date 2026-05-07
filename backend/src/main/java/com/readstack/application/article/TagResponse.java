package com.readstack.application.article;

import com.readstack.domain.article.Tag;

import java.time.Instant;
import java.util.UUID;

public record TagResponse(
        UUID id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getCreatedAt(), tag.getUpdatedAt());
    }
}
