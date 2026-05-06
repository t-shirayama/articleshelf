<script setup lang="ts">
import { Plus, Search } from 'lucide-vue-next'
import type { ArticleSort, ArticleStatus } from '../types'

withDefaults(defineProps<{
  search?: string
  status?: ArticleStatus
  sort?: ArticleSort
}>(), {
  search: '',
  status: 'ALL',
  sort: 'CREATED_DESC'
})

const emit = defineEmits<{
  'update:search': [value: string]
  'update:status': [value: ArticleStatus]
  'update:sort': [value: ArticleSort]
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

    <div
      class="status-tabs"
      role="radiogroup"
      aria-label="ステータスフィルター"
    >
      <button
        type="button"
        class="status-tab"
        :class="{ 'is-selected': status === 'ALL' }"
        :aria-checked="status === 'ALL'"
        role="radio"
        @click="emit('update:status', 'ALL')"
      >
        すべて
      </button>
      <button
        type="button"
        class="status-tab"
        :class="{ 'is-selected': status === 'UNREAD' }"
        :aria-checked="status === 'UNREAD'"
        role="radio"
        @click="emit('update:status', 'UNREAD')"
      >
        未読
      </button>
      <button
        type="button"
        class="status-tab"
        :class="{ 'is-selected': status === 'READ' }"
        :aria-checked="status === 'READ'"
        role="radio"
        @click="emit('update:status', 'READ')"
      >
        読了
      </button>
    </div>

    <VSelect
      class="sort-select"
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

    <VBtn color="primary" @click="emit('add')">
      <template #prepend>
        <Plus :size="18" />
      </template>
      記事を追加
    </VBtn>
  </div>
</template>
