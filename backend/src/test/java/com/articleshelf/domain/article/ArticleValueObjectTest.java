package com.articleshelf.domain.article;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleValueObjectTest {
    @Test
    void normalizesArticleRatingsIntoSupportedRange() {
        assertThat(ArticleRating.normalize((Integer) null)).isZero();
        assertThat(new ArticleRating(-1).value()).isZero();
        assertThat(new ArticleRating(3).value()).isEqualTo(3);
        assertThat(new ArticleRating(9).value()).isEqualTo(ArticleRating.MAX);
    }

    @Test
    void normalizesAndRejectsArticleUrls() {
        assertThat(new ArticleUrl(" https://example.com/article ").value()).isEqualTo("https://example.com/article");

        assertThatThrownBy(() -> ArticleUrl.normalize(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Article URL must not be blank");
        assertThatThrownBy(() -> ArticleUrl.normalize(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizesAndRejectsTagNames() {
        assertThat(new TagName(" Vue ").value()).isEqualTo("Vue");

        assertThatThrownBy(() -> TagName.normalize(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag name must not be blank");
        assertThatThrownBy(() -> TagName.normalize("x".repeat(TagName.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag name must be 255 characters or fewer");
    }
}
