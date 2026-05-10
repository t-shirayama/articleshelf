<script setup lang="ts">
import MarkdownViewer from './MarkdownViewer.vue'
import type { Article } from '../types'

defineProps<{
  article: Article
  summaryText: string
  notesText: string
}>()
</script>

<template>
  <section class="detail-section">
    <div class="detail-section-header">
      <h3>{{ $t('common.summary') }}</h3>
    </div>
    <p class="detail-body-copy" :class="{ 'is-empty': !article.summary }">
      {{ summaryText }}
    </p>
  </section>

  <section class="detail-section">
    <div class="detail-section-header">
      <h3>{{ $t('common.tags') }}</h3>
    </div>
    <div v-if="article.tags.length > 0" class="tag-list detail-tag-list">
      <VChip v-for="tag in article.tags" :key="tag.id || tag.name" size="small" color="secondary" variant="flat">
        {{ tag.name }}
      </VChip>
    </div>
    <p v-else class="detail-body-copy is-empty">{{ $t('detail.emptyTags') }}</p>
  </section>

  <section class="detail-section">
    <div class="detail-section-header">
      <h3>{{ $t('common.notes') }}</h3>
    </div>
    <MarkdownViewer v-if="article.notes" :source="article.notes" />
    <p v-else class="detail-body-copy detail-notes-copy is-empty">{{ notesText }}</p>
  </section>
</template>
