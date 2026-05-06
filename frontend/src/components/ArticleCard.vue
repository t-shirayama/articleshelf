<script setup lang="ts">
import { Bookmark, CalendarDays, CheckCircle2, Circle, Heart, Star, Trash2 } from 'lucide-vue-next'
import { ref, watch } from 'vue'
import type { Article } from '../types'

const props = withDefaults(defineProps<{
  article: Article
  selected?: boolean
}>(), {
  selected: false
})

const emit = defineEmits<{
  click: []
  delete: []
  'toggle-favorite': []
}>()

const thumbnailFailed = ref(false)

watch(() => props.article.thumbnailUrl, () => {
  thumbnailFailed.value = false
})

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
        <img
          v-if="article.thumbnailUrl && !thumbnailFailed"
          :src="article.thumbnailUrl"
          :alt="`${article.title} のサムネイル`"
          loading="lazy"
          @error="thumbnailFailed = true"
        >
        <Bookmark v-else :size="24" />
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
            <VBtn
              class="card-delete-button"
              color="error"
              icon
              size="small"
              variant="text"
              title="記事を削除"
              @click.stop="emit('delete')"
              @keydown.enter.stop="emit('delete')"
              @keydown.space.stop.prevent="emit('delete')"
            >
              <Trash2 :size="16" />
            </VBtn>
          </div>
        </div>
        <h3>{{ article.title }}</h3>
        <p>{{ article.summary || article.notes || '概要やメモはまだありません。' }}</p>
        <div class="article-card-footer">
          <div class="article-meta-group">
            <div class="rating-inline" :class="{ 'is-empty': article.rating <= 0 }" :aria-label="`おすすめ度 ${article.rating} / 5`">
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
