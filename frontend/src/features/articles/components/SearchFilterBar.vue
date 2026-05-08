<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
const sortOptions = computed<Array<{ title: string, value: ArticleSort }>>(() => [
  { title: t('articles.sort.createdDesc'), value: 'CREATED_DESC' },
  { title: t('articles.sort.createdAsc'), value: 'CREATED_ASC' },
  { title: t('articles.sort.updatedDesc'), value: 'UPDATED_DESC' },
  { title: t('articles.sort.readDateDesc'), value: 'READ_DATE_DESC' },
  { title: t('articles.sort.titleAsc'), value: 'TITLE_ASC' },
  { title: t('articles.sort.ratingDesc'), value: 'RATING_DESC' }
])
</script>

<template>
  <div class="search-filter-bar">
    <VTextField
      class="search-input"
      :model-value="search"
      type="text"
      clearable
      hide-details
      :placeholder="t('articles.searchPlaceholder')"
      @update:model-value="emit('update:search', String($event || ''))"
    >
      <template #prepend-inner>
        <Search :size="18" />
      </template>
    </VTextField>

    <VSelect
      class="articleshelf-select sort-select"
      :model-value="sort"
      :items="sortOptions"
      item-title="title"
      item-value="value"
      hide-details
      density="comfortable"
      variant="outlined"
      :label="t('articles.sortLabel')"
      @update:model-value="emit('update:sort', $event as ArticleSort)"
    />

    <VBtn class="action-button action-button-secondary filter-open-button" variant="outlined" @click="emit('openFilters')">
      <template #prepend>
        <SlidersHorizontal :size="18" />
      </template>
      {{ t('common.filter') }}
      <span v-if="activeFilterCount > 0" class="filter-open-badge">{{ activeFilterCount }}</span>
    </VBtn>

    <VBtn class="action-button action-button-primary article-add-button" color="primary" variant="flat" @click="emit('add')">
      <template #prepend>
        <Plus :size="18" />
      </template>
      {{ t('articles.add') }}
    </VBtn>
  </div>

  <div v-if="filterSummary.length > 0" class="filter-summary-row" aria-live="polite">
    <span class="filter-summary-label">{{ t('common.active') }}</span>
    <VChip v-for="item in filterSummary" :key="item" class="filter-summary-chip" size="small" variant="tonal">
      {{ item }}
    </VChip>
  </div>
</template>
