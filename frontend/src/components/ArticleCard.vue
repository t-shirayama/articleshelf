<script setup lang="ts">
import { Bookmark, CalendarDays, CheckCircle2, Circle, Heart, Star } from 'lucide-vue-next'
import type { Article } from '../types'

withDefaults(defineProps<{
  article: Article
  selected?: boolean
}>(), {
  selected: false
})

const emit = defineEmits<{
  click: []
  'toggle-favorite': []
}>()

function domainFrom(url: string): string {
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
          <div class="article-card-actions">
            <VChip :color="article.status === 'READ' ? 'success' : 'warning'" size="small" variant="tonal">
              <CheckCircle2 v-if="article.status === 'READ'" :size="14" />
              <Circle v-else :size="14" />
              {{ article.status === 'READ' ? '読了' : '未読' }}
            </VChip>
            <VBtn
              class="favorite-button"
              :color="article.favorite ? 'primary' : undefined"
              icon
              size="small"
              variant="text"
              :title="article.favorite ? 'お気に入りを解除' : 'お気に入りに追加'"
              @click.stop="emit('toggle-favorite')"
              @keydown.enter.stop="emit('toggle-favorite')"
              @keydown.space.stop.prevent="emit('toggle-favorite')"
            >
              <Heart :size="17" :fill="article.favorite ? 'currentColor' : 'none'" />
            </VBtn>
          </div>
        </div>
        <h3>{{ article.title }}</h3>
        <p>{{ article.summary || article.notes || '概要やメモはまだありません。' }}</p>
        <div class="article-card-footer">
          <div class="article-meta-group">
            <div v-if="article.rating > 0" class="rating-inline" :aria-label="`おすすめ度 ${article.rating} / 5`">
              <Star v-for="star in 5" :key="star" :size="14" :fill="star <= article.rating ? 'currentColor' : 'none'" />
            </div>
            <div class="tag-list">
              <VChip v-for="tag in article.tags" :key="tag.id || tag.name" size="small" variant="tonal">{{ tag.name }}</VChip>
            </div>
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
