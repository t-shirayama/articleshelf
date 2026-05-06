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

const emit = defineEmits(['click'])

function domainFrom(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return url
  }
}
</script>

<template>
  <VCard
    class="article-card"
    :class="{ selected }"
    hover
    role="button"
    tabindex="0"
    @click="emit('click')"
    @keydown.enter="emit('click')"
    @keydown.space.prevent="emit('click')"
  >
    <div class="article-card-content">
      <div class="article-thumb">
        <Bookmark :size="24" />
      </div>
      <div class="article-card-body">
        <div class="article-card-topline">
          <span class="domain">{{ domainFrom(article.url) }}</span>
          <VChip :color="article.status === 'READ' ? 'success' : 'warning'" size="small" variant="tonal">
            <CheckCircle2 v-if="article.status === 'READ'" :size="14" />
            <Circle v-else :size="14" />
            {{ article.status === 'READ' ? '読了' : '未読' }}
          </VChip>
        </div>
        <h3>{{ article.title }}</h3>
        <p>{{ article.summary || article.notes || '概要やメモはまだありません。' }}</p>
        <div class="article-card-footer">
          <div class="tag-list">
            <VChip v-for="tag in article.tags" :key="tag.id || tag.name" size="small" variant="tonal">{{ tag.name }}</VChip>
          </div>
          <span v-if="article.readDate" class="date-meta">
            <CalendarDays :size="14" />
            {{ article.readDate }}
          </span>
        </div>
      </div>
    </div>
  </VCard>
</template>
