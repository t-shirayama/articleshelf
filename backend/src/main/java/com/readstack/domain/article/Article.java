package com.readstack.domain.article;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Article {
    private final UUID id;
    private String url;
    private String title;
    private String summary;
    private ArticleStatus status;
    private LocalDate readDate;
    private boolean favorite;
    private String notes;
    private Set<Tag> tags;
    private Instant createdAt;
    private Instant updatedAt;

    public Article(
            UUID id,
            String url,
            String title,
            String summary,
            ArticleStatus status,
            LocalDate readDate,
            boolean favorite,
            String notes,
            Set<Tag> tags,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.url = url;
        this.title = title;
        this.summary = summary == null ? "" : summary;
        this.status = status == null ? ArticleStatus.UNREAD : status;
        this.readDate = readDate;
        this.favorite = favorite;
        this.notes = notes == null ? "" : notes;
        this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public ArticleStatus getStatus() {
        return status;
    }

    public LocalDate getReadDate() {
        return readDate;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public String getNotes() {
        return notes;
    }

    public Set<Tag> getTags() {
        return new LinkedHashSet<>(tags);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
