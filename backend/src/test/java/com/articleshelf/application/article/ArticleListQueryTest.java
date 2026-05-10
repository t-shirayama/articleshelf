package com.articleshelf.application.article;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleListQueryTest {
    @Test
    void leavesItemsUnchangedWhenPaginationIsNotRequested() {
        ArticleListQuery query = new ArticleListQuery(null, null, null);

        assertThat(query.slice(List.of("a", "b", "c"))).containsExactly("a", "b", "c");
    }

    @Test
    void normalizesPageAndSizeBeforeSlicingItems() {
        ArticleListQuery query = new ArticleListQuery(-1, 500, "createdDesc");

        assertThat(query.normalizedPage()).isZero();
        assertThat(query.normalizedSize()).isEqualTo(200);
        assertThat(query.slice(List.of("a", "b", "c"))).containsExactly("a", "b", "c");
    }

    @Test
    void slicesRequestedPage() {
        ArticleListQuery query = new ArticleListQuery(1, 2, null);

        assertThat(query.slice(List.of("a", "b", "c", "d", "e"))).containsExactly("c", "d");
    }
}
