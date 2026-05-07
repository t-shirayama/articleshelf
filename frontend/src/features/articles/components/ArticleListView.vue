<script setup lang="ts">
import ArticleCard from "./ArticleCard.vue";
import SearchFilterBar from "./SearchFilterBar.vue";
import type { Article, ArticleSort } from "../types";

defineProps<{
  title: string;
  search: string;
  sort: ArticleSort;
  filterSummary: string[];
  activeFilterCount: number;
  error: string;
  loading: boolean;
  articles: Article[];
  selectedArticleId?: string;
}>();

const emit = defineEmits<{
  "update:search": [value: string];
  "update:sort": [value: ArticleSort];
  openFilters: [];
  add: [];
  openArticle: [article: Article];
  deleteArticle: [article: Article];
  toggleStatus: [article: Article];
  toggleFavorite: [article: Article];
  retry: [];
}>();
</script>

<template>
    <header class="page-header">
      <div>
        <h1>{{ title }}</h1>
      </div>
      <SearchFilterBar
        :search="search"
        :sort="sort"
        :filter-summary="filterSummary"
        :active-filter-count="activeFilterCount"
        @update:search="emit('update:search', $event)"
        @update:sort="emit('update:sort', $event)"
        @open-filters="emit('openFilters')"
        @add="emit('add')"
      />
    </header>

    <div
      v-if="error"
      class="error-banner"
      role="alert"
      aria-live="assertive"
    >
      <strong>データを読み込めませんでした</strong>
      <span>{{ error }}</span>
      <div class="error-banner-actions">
        <VBtn
          class="action-button action-button-secondary error-banner-action"
          variant="outlined"
          size="small"
          @click="emit('retry')"
        >
          再試行
        </VBtn>
      </div>
    </div>

    <section class="article-list" aria-live="polite">
      <div v-if="loading" class="loading-state">
        <VProgressCircular indeterminate color="primary" size="42" />
      </div>
      <div v-else-if="articles.length === 0" class="empty-state">
        <h2>まだ記事がありません</h2>
        <p>URL を貼り付けて、学びの断片をここに積み上げていきましょう。</p>
        <VBtn
          class="action-button action-button-primary"
          color="primary"
          variant="flat"
          @click="emit('add')"
          >最初の記事を追加</VBtn
        >
      </div>
      <template v-else>
        <ArticleCard
          v-for="article in articles"
          :key="article.id"
          :article="article"
          :selected="selectedArticleId === article.id"
          @click="emit('openArticle', article)"
          @delete="emit('deleteArticle', article)"
          @toggle-status="emit('toggleStatus', article)"
          @toggle-favorite="emit('toggleFavorite', article)"
        />
      </template>
    </section>
</template>
