package com.articleshelf.domain.article;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleTest {
    @Test
    void createsUnreadArticleAndClampsRatingWhenOptionalValuesAreMissing() {
        UUID userId = UUID.randomUUID();

        Article article = new Article(
                null,
                userId,
                "https://example.com/article",
                "Title",
                null,
                null,
                null,
                null,
                false,
                9,
                null,
                Set.of(),
                null,
                null
        );

        assertThat(article.getId()).isNotNull();
        assertThat(article.getUserId()).isEqualTo(userId);
        assertThat(article.getStatus()).isEqualTo(ArticleStatus.UNREAD);
        assertThat(article.getSummary()).isEmpty();
        assertThat(article.getThumbnailUrl()).isEmpty();
        assertThat(article.getNotes()).isEmpty();
        assertThat(article.getRating()).isEqualTo(5);
    }
}
