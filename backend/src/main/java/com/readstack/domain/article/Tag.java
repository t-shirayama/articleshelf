package com.readstack.domain.article;

import java.time.Instant;
import java.util.UUID;

public class Tag {
    private final UUID id;
    private final String name;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Tag(UUID id, String name, Instant createdAt, Instant updatedAt) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.name = name == null ? "" : name.trim();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
