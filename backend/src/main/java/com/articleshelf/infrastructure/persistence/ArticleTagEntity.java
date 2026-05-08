package com.articleshelf.infrastructure.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "article_tags")
public class ArticleTagEntity {
    @EmbeddedId
    private ArticleTagId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "article_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false),
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, insertable = false, updatable = false)
    })
    private ArticleEntity article;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumns({
            @JoinColumn(name = "tag_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false),
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, insertable = false, updatable = false)
    })
    private TagEntity tag;

    public static ArticleTagEntity link(ArticleEntity article, TagEntity tag) {
        ArticleTagEntity articleTag = new ArticleTagEntity();
        articleTag.id = new ArticleTagId(article.getUserId(), article.getId(), tag.getId());
        articleTag.article = article;
        articleTag.tag = tag;
        return articleTag;
    }

    public ArticleTagId getId() {
        return id;
    }

    public ArticleEntity getArticle() {
        return article;
    }

    public TagEntity getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ArticleTagEntity that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
