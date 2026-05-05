<script setup>
import { Bookmark, CalendarDays, CheckCircle2, Circle } from 'lucide-vue-next'

defineProps({
  article: {
    type: Object,
    required: true
  },
  selected: {
    type: Boolean,
    default: false
  }
})

function domainFrom(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return url
  }
}
</script>

<template>
  <button class="article-card" :class="{ selected }">
    <div class="article-thumb">
      <Bookmark :size="24" />
    </div>
    <div class="article-card-body">
      <div class="article-card-topline">
        <span class="domain">{{ domainFrom(article.url) }}</span>
        <span class="status-pill" :class="article.status.toLowerCase()">
          <CheckCircle2 v-if="article.status === 'READ'" :size="14" />
          <Circle v-else :size="14" />
          {{ article.status === 'READ' ? '読了' : '未読' }}
        </span>
      </div>
      <h3>{{ article.title }}</h3>
      <p>{{ article.summary || article.notes || '概要やメモはまだありません。' }}</p>
      <div class="article-card-footer">
        <div class="tag-list">
          <span v-for="tag in article.tags" :key="tag.id || tag.name" class="tag-chip">{{ tag.name }}</span>
        </div>
        <span v-if="article.readDate" class="date-meta">
          <CalendarDays :size="14" />
          {{ article.readDate }}
        </span>
      </div>
    </div>
  </button>
</template>
