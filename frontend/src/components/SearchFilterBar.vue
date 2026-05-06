<script setup lang="ts">
import { Plus, Search, SlidersHorizontal } from 'lucide-vue-next'
import type { ArticleSort } from '../types'

withDefaults(defineProps<{
  search?: string
  sort?: ArticleSort
  filterSummary?: string[]
  activeFilterCount?: number
}>(), {
  search: '',
  sort: 'CREATED_DESC',
  filterSummary: () => [],
  activeFilterCount: 0
})

const emit = defineEmits<{
  'update:search': [value: string]
  'update:sort': [value: ArticleSort]
  openFilters: []
  add: []
}>()

const sortOptions = [
  { title: '新しい順', value: 'CREATED_DESC' },
  { title: '古い順', value: 'CREATED_ASC' },
  { title: '更新順', value: 'UPDATED_DESC' },
  { title: '読了日順', value: 'READ_DATE_DESC' },
  { title: 'タイトル順', value: 'TITLE_ASC' },
  { title: 'おすすめ順', value: 'RATING_DESC' }
] satisfies Array<{ title: string, value: ArticleSort }>
</script>

<template>
  <div class="search-filter-bar">
    <VTextField
      class="search-input"
      :model-value="search"
      type="text"
      clearable
      hide-details
      placeholder="タイトル・URL・メモで検索"
      @update:model-value="emit('update:search', String($event || ''))"
    >
      <template #prepend-inner>
        <Search :size="18" />
      </template>
    </VTextField>

    <VSelect
      class="readstack-select sort-select"
      :model-value="sort"
      :items="sortOptions"
      item-title="title"
      item-value="value"
      hide-details
      density="comfortable"
      variant="outlined"
      label="並び順"
      @update:model-value="emit('update:sort', $event as ArticleSort)"
    />

    <VBtn class="action-button action-button-secondary filter-open-button" variant="outlined" @click="emit('openFilters')">
      <template #prepend>
        <SlidersHorizontal :size="18" />
      </template>
      フィルタ
      <span v-if="activeFilterCount > 0" class="filter-open-badge">{{ activeFilterCount }}</span>
    </VBtn>

    <VBtn class="action-button action-button-primary article-add-button" color="primary" variant="flat" @click="emit('add')">
      <template #prepend>
        <Plus :size="18" />
      </template>
      記事を追加
    </VBtn>
  </div>

  <div v-if="filterSummary.length > 0" class="filter-summary-row" aria-live="polite">
    <span class="filter-summary-label">適用中</span>
    <VChip v-for="item in filterSummary" :key="item" class="filter-summary-chip" size="small" variant="tonal">
      {{ item }}
    </VChip>
  </div>
</template>
