package com.articleshelf.application.article;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleListQueryTest {
    @Test
    void usesCreatedDescAsTheDefaultSort() {
        ArticleListQuery query = new ArticleListQuery(null, null, null);

        assertThat(query.paged()).isFalse();
        assertThat(query.normalizedSort()).isEqualTo(ArticleListQuery.SortKey.CREATED_DESC);
    }

    @Test
    void normalizesPageAndSizeWithinSupportedBounds() {
        ArticleListQuery query = new ArticleListQuery(-1, 500, "createdDesc");

        assertThat(query.normalizedPage()).isZero();
        assertThat(query.normalizedSize()).isEqualTo(200);
        assertThat(query.normalizedSort()).isEqualTo(ArticleListQuery.SortKey.CREATED_DESC);
    }

    @Test
    void acceptsDocumentedSortKeysAndFallsBackForUnknownValues() {
        assertThat(new ArticleListQuery(null, null, "CREATED_ASC").normalizedSort())
                .isEqualTo(ArticleListQuery.SortKey.CREATED_ASC);
        assertThat(new ArticleListQuery(null, null, "UPDATED_DESC").normalizedSort())
                .isEqualTo(ArticleListQuery.SortKey.UPDATED_DESC);
        assertThat(new ArticleListQuery(null, null, "READ_DATE_DESC").normalizedSort())
                .isEqualTo(ArticleListQuery.SortKey.READ_DATE_DESC);
        assertThat(new ArticleListQuery(null, null, "TITLE_ASC").normalizedSort())
                .isEqualTo(ArticleListQuery.SortKey.TITLE_ASC);
        assertThat(new ArticleListQuery(null, null, "RATING_DESC").normalizedSort())
                .isEqualTo(ArticleListQuery.SortKey.RATING_DESC);

        assertThat(new ArticleListQuery(null, null, "not-a-sort").normalizedSort())
                .isEqualTo(ArticleListQuery.SortKey.CREATED_DESC);
    }
}
