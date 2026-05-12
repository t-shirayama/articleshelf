package com.articleshelf.domain.article;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Article {
    private final UUID id;
    private final UUID userId;
    private final long version;
    private String url;
    private String title;
    private String summary;
    private String thumbnailUrl;
    private ArticleStatus status;
    private LocalDate readDate;
    private boolean favorite;
    private int rating;
    private String notes;
    private Set<Tag> tags;
    private Instant createdAt;
    private Instant updatedAt;

    public Article(
            UUID id,
            UUID userId,
            long version,
            String url,
            String title,
            String summary,
            String thumbnailUrl,
            ArticleStatus status,
            LocalDate readDate,
            boolean favorite,
            int rating,
            String notes,
            Set<Tag> tags,
            Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.userId = userId;
        this.version = version;
        this.url = ArticleUrl.normalize(url);
        this.title = normalizeText(title);
        this.summary = normalizeText(summary);
        this.thumbnailUrl = normalizeText(thumbnailUrl);
        this.status = status == null ? ArticleStatus.UNREAD : status;
        this.readDate = readDate;
        this.favorite = favorite;
        this.rating = ArticleRating.normalize(rating);
        this.notes = normalizeText(notes);
        this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateContent(String url, String title, String summary, String notes) {
        this.url = ArticleUrl.normalize(url);
        this.title = normalizeText(title);
        this.summary = normalizeText(summary);
        this.notes = normalizeText(notes);
    }

    public void changeThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = normalizeText(thumbnailUrl);
    }

    public void changeStatus(ArticleStatus status, LocalDate readDate) {
        this.status = status == null ? ArticleStatus.UNREAD : status;
        this.readDate = readDate;
    }

    public void changeFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void changeRating(int rating) {
        this.rating = ArticleRating.normalize(rating);
    }

    public void replaceTags(Set<Tag> tags) {
        this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public long getVersion() {
        return version;
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

    public String getThumbnailUrl() {
        return thumbnailUrl;
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

    public int getRating() {
        return rating;
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

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
