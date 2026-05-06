<script setup lang="ts">
import { Plus, Search, SlidersHorizontal } from 'lucide-vue-next'
import type { ArticleStatus } from '../types'

withDefaults(defineProps<{
  search?: string
  status?: ArticleStatus
}>(), {
  search: '',
  status: 'ALL'
})

const emit = defineEmits<{
  'update:search': [value: string]
  'update:status': [value: ArticleStatus]
  add: []
}>()
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

    <VBtnToggle
      class="status-tabs"
      :model-value="status"
      mandatory
      density="comfortable"
      aria-label="ステータスフィルター"
      @update:model-value="emit('update:status', $event as ArticleStatus)"
    >
      <VBtn value="ALL">すべて</VBtn>
      <VBtn value="UNREAD">未読</VBtn>
      <VBtn value="READ">読了</VBtn>
    </VBtnToggle>

    <VBtn icon variant="text" title="フィルター">
      <SlidersHorizontal :size="18" />
    </VBtn>

    <VBtn color="primary" @click="emit('add')">
      <template #prepend>
        <Plus :size="18" />
      </template>
      記事を追加
    </VBtn>
  </div>
</template>
