<script setup lang="ts">
import { Bookmark, CalendarDays, CheckCircle2, Circle, Heart, Star, Trash2 } from 'lucide-vue-next'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { loadThumbnailFromCache } from '../services/thumbnailCache'
import type { Article } from '../types'
import { formatDate } from '../utils/dateFormat'

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
const thumbnailRoot = ref<HTMLElement | null>(null)
const thumbnailSrc = ref('')
const shouldLoadThumbnail = ref(false)
let objectUrl = ''
let loadVersion = 0
let thumbnailObserver: IntersectionObserver | null = null

watch([() => props.article.thumbnailUrl, shouldLoadThumbnail], async ([thumbnailUrl, shouldLoad]) => {
  const version = ++loadVersion
  revokeObjectUrl()
  thumbnailFailed.value = false
  thumbnailSrc.value = ''

  if (!thumbnailUrl || !shouldLoad) return

  const cachedUrl = await loadThumbnailFromCache(thumbnailUrl)
  if (version !== loadVersion) {
    if (cachedUrl) URL.revokeObjectURL(cachedUrl)
    return
  }

  if (!cachedUrl) {
    thumbnailFailed.value = true
    return
  }

  objectUrl = cachedUrl
  thumbnailSrc.value = cachedUrl
}, { immediate: true })

onMounted(() => {
  if (!thumbnailRoot.value || !('IntersectionObserver' in window)) {
    shouldLoadThumbnail.value = true
    return
  }

  thumbnailObserver = new IntersectionObserver((entries) => {
    if (!entries.some((entry) => entry.isIntersecting)) return
    shouldLoadThumbnail.value = true
    thumbnailObserver?.disconnect()
    thumbnailObserver = null
  }, { rootMargin: '160px' })

  thumbnailObserver.observe(thumbnailRoot.value)
})

onBeforeUnmount(() => {
  loadVersion += 1
  thumbnailObserver?.disconnect()
  revokeObjectUrl()
})

function revokeObjectUrl(): void {
  if (!objectUrl) return
  URL.revokeObjectURL(objectUrl)
  objectUrl = ''
}

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
      <div ref="thumbnailRoot" class="article-thumb">
        <img
          v-if="thumbnailSrc && !thumbnailFailed"
          :src="thumbnailSrc"
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
            <div class="article-meta-line">
              <div class="rating-inline" :class="{ 'is-empty': article.rating <= 0 }" :aria-label="`おすすめ度 ${article.rating} / 5`">
                <Star v-for="star in 5" :key="star" :size="14" :fill="star <= article.rating ? 'currentColor' : 'none'" />
              </div>
              <div class="card-date-list" aria-label="記事の日付">
                <CalendarDays :size="14" />
                <div class="card-date-values">
                  <span v-if="article.createdAt" class="date-meta">登録日 {{ formatDate(article.createdAt) }}</span>
                  <span class="date-meta">読了日 {{ formatDate(article.readDate) }}</span>
                </div>
              </div>
            </div>
            <div class="tag-list">
              <VChip v-for="tag in article.tags" :key="tag.id || tag.name" size="small" variant="tonal">{{ tag.name }}</VChip>
            </div>
          </div>
        </div>
      </div>
    </div>
  </VCard>
</template>
