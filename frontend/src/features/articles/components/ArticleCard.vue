<script setup lang="ts">
import { Bookmark, CalendarDays, Check, CheckCircle2, Circle, Heart, Star, Trash2 } from 'lucide-vue-next'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { loadThumbnailFromCache } from '../../../shared/services/thumbnailCache'
import type { Article } from '../types'
import { formatDate } from '../../../shared/utils/dateFormat'

const props = withDefaults(defineProps<{
  article: Article
  selected?: boolean
}>(), {
  selected: false
})

const emit = defineEmits<{
  click: []
  delete: []
  'toggle-status': []
  'toggle-favorite': []
}>()

const { t } = useI18n()
const thumbnailFailed = ref(false)
const thumbnailRoot = ref<HTMLElement | null>(null)
const thumbnailSrc = ref('')
const shouldLoadThumbnail = ref(false)
const visibleTags = computed(() => props.article.tags.slice(0, 3))
const hiddenTagCount = computed(() => Math.max(props.article.tags.length - visibleTags.value.length, 0))
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
  >
    <div class="article-card-content">
      <button
        class="article-card-open"
        type="button"
        :aria-label="t('articles.openArticleDetail', { title: article.title })"
        @click="emit('click')"
      >
        <div ref="thumbnailRoot" class="article-thumb">
          <img
            v-if="thumbnailSrc && !thumbnailFailed"
            :src="thumbnailSrc"
            :alt="t('articles.thumbnailAlt', { title: article.title })"
            loading="lazy"
            @error="thumbnailFailed = true"
          >
          <Bookmark v-else :size="24" />
        </div>
        <div class="article-card-body">
          <div class="article-card-topline">
            <span class="domain">{{ domainFrom(article.url) }}</span>
          </div>
          <h3>{{ article.title }}</h3>
          <p>{{ article.summary || article.notes || '' }}</p>
          <div class="article-card-footer">
            <div class="article-meta-group">
              <div class="article-meta-line">
                <div class="rating-inline" :class="{ 'is-empty': article.rating <= 0 }" :aria-label="t('common.ratingValue', { value: article.rating })">
                  <Star v-for="star in 5" :key="star" :size="14" :fill="star <= article.rating ? 'currentColor' : 'none'" />
                </div>
                <div class="tag-list article-card-tag-list">
                  <VChip v-for="tag in visibleTags" :key="tag.id || tag.name" size="small" variant="tonal">{{ tag.name }}</VChip>
                  <VChip v-if="hiddenTagCount > 0" class="article-card-more-tags" size="small" variant="tonal">
                    +{{ hiddenTagCount }}
                  </VChip>
                </div>
                <div class="article-card-status-date">
                  <div class="status-toggle-group">
                    <VChip class="status-chip" :color="article.status === 'READ' ? 'success' : 'warning'" size="small" variant="tonal">
                      <CheckCircle2 v-if="article.status === 'READ'" :size="14" />
                      <Circle v-else :size="14" />
                      {{ article.status === 'READ' ? t('articles.statusRead') : t('articles.statusUnread') }}
                    </VChip>
                  </div>
                  <div class="card-date-list" :aria-label="t('articles.dateListLabel')">
                    <CalendarDays :size="14" />
                    <div class="card-date-values">
                      <span v-if="article.createdAt" class="date-meta">{{ t('articles.createdDate') }} {{ formatDate(article.createdAt) }}</span>
                      <span class="date-meta">{{ t('articles.readDate') }} {{ formatDate(article.readDate) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </button>
      <div class="article-card-actions">
        <VBtn
          class="status-toggle-button"
          :class="{ 'is-active': article.status === 'READ' }"
          icon
          size="small"
          type="button"
          variant="outlined"
          :title="article.status === 'READ' ? t('articles.markUnread') : t('articles.markRead')"
          @click.stop="emit('toggle-status')"
        >
          <Check :size="18" :stroke-width="3" />
        </VBtn>
        <VBtn
          class="favorite-button card-favorite-button"
          :class="{ 'is-active': article.favorite }"
          icon
          size="small"
          variant="text"
          :title="article.favorite ? t('articles.removeFavorite') : t('articles.addFavorite')"
          @click.stop="emit('toggle-favorite')"
        >
          <Heart :size="17" :fill="article.favorite ? 'currentColor' : 'none'" />
        </VBtn>
        <VBtn
          class="card-delete-button"
          color="error"
          icon
          size="small"
          variant="text"
          :title="t('articles.deleteArticle')"
          @click.stop="emit('delete')"
        >
          <Trash2 :size="16" />
        </VBtn>
      </div>
    </div>
  </VCard>
</template>
