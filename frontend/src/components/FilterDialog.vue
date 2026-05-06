<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { ArticleDateRange } from '../types'

interface FilterDraft {
  tags: string[]
  ratings: number[]
  createdRange: ArticleDateRange
  readRange: ArticleDateRange
}

const props = defineProps<{
  open: boolean
  availableTags: string[]
  filters: FilterDraft
}>()

const emit = defineEmits<{
  close: []
  apply: [value: FilterDraft]
}>()

const draft = reactive<FilterDraft>(createDraft(props.filters))

watch(() => props.open, (isOpen) => {
  if (isOpen) {
    syncDraft(props.filters)
  }
})

watch(() => props.filters, (value) => {
  if (props.open) {
    syncDraft(value)
  }
}, { deep: true })

const ratingOptions = [5, 4, 3, 2, 1]
const hasAnyDraft = computed(() => (
  draft.tags.length > 0 ||
  draft.ratings.length > 0 ||
  Boolean(draft.createdRange.from) ||
  Boolean(draft.createdRange.to) ||
  Boolean(draft.readRange.from) ||
  Boolean(draft.readRange.to)
))

function syncDraft(value: FilterDraft): void {
  draft.tags = [...value.tags]
  draft.ratings = [...value.ratings]
  draft.createdRange = { ...value.createdRange }
  draft.readRange = { ...value.readRange }
}

function resetDraft(): void {
  syncDraft(createDraft())
}

function applyFilters(): void {
  emit('apply', createDraft(draft))
}

function createDraft(value?: Partial<FilterDraft>): FilterDraft {
  return {
    tags: [...(value?.tags || [])],
    ratings: [...(value?.ratings || [])],
    createdRange: {
      from: value?.createdRange?.from || '',
      to: value?.createdRange?.to || ''
    },
    readRange: {
      from: value?.readRange?.from || '',
      to: value?.readRange?.to || ''
    }
  }
}
</script>

<template>
  <VDialog :model-value="open" max-width="640" @update:model-value="value => { if (!value) emit('close') }">
    <VCard class="filter-dialog" title="フィルタ">
      <VCardText class="filter-dialog-body">
        <div class="filter-dialog-section">
          <div class="filter-dialog-header">
            <strong>タグ</strong>
            <span>複数選択したタグのいずれかに一致する記事を表示します</span>
          </div>
          <VAutocomplete
            v-model="draft.tags"
            class="readstack-select"
            :items="availableTags"
            chips
            closable-chips
            clearable
            hide-details
            label="タグを選択"
            multiple
            variant="outlined"
          />
        </div>

        <div class="filter-dialog-section">
          <div class="filter-dialog-header">
            <strong>おすすめ度</strong>
            <span>該当する星の数を複数選べます</span>
          </div>
          <div class="filter-rating-grid">
            <VBtn
              v-for="rating in ratingOptions"
              :key="rating"
              class="filter-rating-chip"
              :class="{ 'is-active': draft.ratings.includes(rating) }"
              variant="outlined"
              @click="draft.ratings = draft.ratings.includes(rating) ? draft.ratings.filter(value => value !== rating) : [...draft.ratings, rating].sort((left, right) => left - right)"
            >
              {{ rating }} / 5
            </VBtn>
          </div>
        </div>

        <div class="filter-dialog-section">
          <div class="filter-dialog-header">
            <strong>登録日</strong>
            <span>期間を指定して追加タイミングで絞り込みます</span>
          </div>
          <div class="filter-date-grid">
            <VTextField
              v-model="draft.createdRange.from"
              class="readstack-select"
              type="date"
              hide-details
              label="開始日"
              variant="outlined"
            />
            <VTextField
              v-model="draft.createdRange.to"
              class="readstack-select"
              type="date"
              hide-details
              label="終了日"
              variant="outlined"
            />
          </div>
        </div>

        <div class="filter-dialog-section">
          <div class="filter-dialog-header">
            <strong>読了日</strong>
            <span>読了済み記事だけを期間で見たいときに使えます</span>
          </div>
          <div class="filter-date-grid">
            <VTextField
              v-model="draft.readRange.from"
              class="readstack-select"
              type="date"
              hide-details
              label="開始日"
              variant="outlined"
            />
            <VTextField
              v-model="draft.readRange.to"
              class="readstack-select"
              type="date"
              hide-details
              label="終了日"
              variant="outlined"
            />
          </div>
        </div>
      </VCardText>

      <VCardActions class="filter-dialog-actions">
        <VBtn class="action-button action-button-secondary" variant="outlined" :disabled="!hasAnyDraft" @click="resetDraft">条件をクリア</VBtn>
        <VSpacer />
        <VBtn class="action-button action-button-secondary" variant="outlined" @click="emit('close')">閉じる</VBtn>
        <VBtn class="action-button action-button-primary" color="primary" variant="flat" @click="applyFilters">適用する</VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
