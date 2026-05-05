<script setup>
import { Plus, Search, SlidersHorizontal } from 'lucide-vue-next'

defineProps({
  search: {
    type: String,
    default: ''
  },
  status: {
    type: String,
    default: 'ALL'
  }
})

const emit = defineEmits(['update:search', 'update:status', 'add'])
</script>

<template>
  <div class="search-filter-bar">
    <label class="search-box">
      <Search :size="18" />
      <input
        :value="search"
        type="search"
        placeholder="タイトル・URL・メモで検索"
        @input="emit('update:search', $event.target.value)"
      />
    </label>

    <div class="status-tabs" aria-label="ステータスフィルター">
      <button :class="{ active: status === 'ALL' }" @click="emit('update:status', 'ALL')">すべて</button>
      <button :class="{ active: status === 'UNREAD' }" @click="emit('update:status', 'UNREAD')">未読</button>
      <button :class="{ active: status === 'READ' }" @click="emit('update:status', 'READ')">読了</button>
    </div>

    <button class="icon-button muted" title="フィルター">
      <SlidersHorizontal :size="18" />
    </button>

    <button class="primary-button" @click="emit('add')">
      <Plus :size="18" />
      記事を追加
    </button>
  </div>
</template>
