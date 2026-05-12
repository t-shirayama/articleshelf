package com.articleshelf.domain.article;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleTest {
    @Test
    void createsUnreadArticleAndClampsRatingWhenOptionalValuesAreMissing() {
        UUID userId = UUID.randomUUID();

        Article article = new Article(
                null,
                userId,
                0L,
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

    @Test
    void updatesContentAndMutableArticleAttributesThroughDomainMethods() {
        UUID userId = UUID.randomUUID();
        Tag tag = new Tag(null, userId, " Vue ", null, null);
        Article article = new Article(
                null,
                userId,
                0L,
                " https://example.com/article ",
                "Title",
                "Summary",
                null,
                ArticleStatus.UNREAD,
                null,
                false,
                1,
                "",
                Set.of(),
                null,
                null
        );

        article.updateContent(" https://example.com/updated ", " Updated ", null, " Notes ");
        article.changeThumbnailUrl(" https://example.com/thumb.png ");
        article.changeStatus(ArticleStatus.READ, java.time.LocalDate.of(2026, 5, 10));
        article.changeFavorite(true);
        article.changeRating(-2);
        article.replaceTags(Set.of(tag));

        assertThat(article.getUrl()).isEqualTo("https://example.com/updated");
        assertThat(article.getTitle()).isEqualTo("Updated");
        assertThat(article.getSummary()).isEmpty();
        assertThat(article.getThumbnailUrl()).isEqualTo("https://example.com/thumb.png");
        assertThat(article.getStatus()).isEqualTo(ArticleStatus.READ);
        assertThat(article.getReadDate()).isEqualTo(java.time.LocalDate.of(2026, 5, 10));
        assertThat(article.isFavorite()).isTrue();
        assertThat(article.getRating()).isZero();
        assertThat(article.getNotes()).isEqualTo("Notes");
        assertThat(article.getTags()).extracting(Tag::getName).containsExactly("Vue");
    }

    @Test
    void rejectsBlankUrlAndTagNameAtDomainBoundary() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> new Article(
                null,
                userId,
                0L,
                " ",
                "Title",
                null,
                null,
                null,
                null,
                false,
                0,
                null,
                Set.of(),
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Tag(null, userId, " ", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
