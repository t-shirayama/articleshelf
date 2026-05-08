package com.articleshelf.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ArticleTagId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "tag_id", nullable = false)
    private UUID tagId;

    public ArticleTagId() {
    }

    public ArticleTagId(UUID userId, UUID articleId, UUID tagId) {
        this.userId = userId;
        this.articleId = articleId;
        this.tagId = tagId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getArticleId() {
        return articleId;
    }

    public UUID getTagId() {
        return tagId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ArticleTagId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId)
                && Objects.equals(articleId, that.articleId)
                && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, articleId, tagId);
    }
}
