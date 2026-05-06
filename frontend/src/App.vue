<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { BookOpen, CheckCircle2, Circle, Heart, Library, Tag } from 'lucide-vue-next'
import ArticleCard from './components/ArticleCard.vue'
import ArticleDetail from './components/ArticleDetail.vue'
import ArticleFormModal from './components/ArticleFormModal.vue'
import MotivationCard from './components/MotivationCard.vue'
import SearchFilterBar from './components/SearchFilterBar.vue'
import { motivationCards } from './data/motivationCards'
import { useArticlesStore } from './stores/articles'
import type { Article, ArticleInput, ArticleSort, ArticleStatus, Tag as ArticleTag } from './types'

const store = useArticlesStore()
const modalOpen = ref(false)
const searchDraft = ref('')
const viewMode = ref<'list' | 'detail'>('list')
const motivationIndex = ref(new Date().getDate() % motivationCards.length)
let searchTimer: ReturnType<typeof window.setTimeout> | undefined

const visibleTags = computed<ArticleTag[]>(() => store.tags)
const currentMotivation = computed(() => motivationCards[motivationIndex.value])

watch(searchDraft, (value) => {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    store.setSearch(value)
  }, 250)
})

onMounted(async () => {
  await Promise.all([store.fetchArticles(), store.fetchTags()])
})

async function createArticle(article: ArticleInput): Promise<void> {
  await store.createArticle(article)
  rotateMotivation()
  modalOpen.value = false
  viewMode.value = 'detail'
}

async function saveArticle(article: ArticleInput): Promise<void> {
  await store.updateArticle(article)
}

async function deleteArticle(articleId: string): Promise<void> {
  await store.deleteArticle(articleId)
  showList()
}

async function openArticle(article: Article): Promise<void> {
  await store.selectArticle(article)
  rotateMotivation()
  viewMode.value = 'detail'
}

async function toggleFavorite(article: Article): Promise<void> {
  await store.toggleFavorite(article)
}

function showList(): void {
  if (viewMode.value !== 'list') rotateMotivation()
  viewMode.value = 'list'
}

function rotateMotivation(): void {
  motivationIndex.value = (motivationIndex.value + 1) % motivationCards.length
}

function setStatus(status: ArticleStatus): Promise<void> {
  showList()
  return store.setStatus(status)
}

function setTag(tag: string): Promise<void> {
  showList()
  return store.setTag(tag)
}

function setFavoriteOnly(): Promise<void> {
  showList()
  return store.setFavoriteOnly()
}

function setSort(sort: ArticleSort): void {
  store.setSort(sort)
}
</script>

<template>
  <VApp>
    <div v-if="viewMode === 'list'" class="app-shell">
      <aside class="sidebar">
        <div class="brand">
          <div class="brand-mark">
            <BookOpen :size="22" />
          </div>
          <div>
            <strong>ReadStack</strong>
            <span>学びを蓄える記事棚</span>
          </div>
        </div>

        <nav class="side-nav">
          <VBtn
            block
            variant="text"
            :color="store.filters.status === 'ALL' && !store.filters.tag && !store.filters.favorite ? 'primary' : undefined"
            @click="setStatus('ALL'); setTag('')"
          >
            <template #prepend>
              <Library :size="18" />
            </template>
            <span>すべての記事</span>
            <strong>{{ store.counts.all }}</strong>
          </VBtn>
          <VBtn
            block
            variant="text"
            :color="store.filters.status === 'UNREAD' ? 'primary' : undefined"
            @click="setStatus('UNREAD')"
          >
            <template #prepend>
              <Circle :size="18" />
            </template>
            <span>未読</span>
            <strong>{{ store.counts.unread }}</strong>
          </VBtn>
          <VBtn
            block
            variant="text"
            :color="store.filters.status === 'READ' ? 'primary' : undefined"
            @click="setStatus('READ')"
          >
            <template #prepend>
              <CheckCircle2 :size="18" />
            </template>
            <span>読了</span>
            <strong>{{ store.counts.read }}</strong>
          </VBtn>
          <VBtn
            block
            variant="text"
            :color="store.filters.favorite ? 'primary' : undefined"
            @click="setFavoriteOnly"
          >
            <template #prepend>
              <Heart :size="18" />
            </template>
            <span>お気に入り</span>
            <strong>{{ store.counts.favorite }}</strong>
          </VBtn>
        </nav>

        <section class="tag-section">
          <h2>タグ</h2>
          <div class="sidebar-tag-list">
            <VBtn
              v-for="tag in visibleTags"
              :key="tag.id"
              block
              variant="text"
              :color="store.filters.tag === tag.name ? 'primary' : undefined"
              @click="setTag(store.filters.tag === tag.name ? '' : tag.name)"
            >
              <template #prepend>
                <Tag :size="16" />
              </template>
              {{ tag.name }}
            </VBtn>
            <p v-if="visibleTags.length === 0" class="muted-note">タグはまだありません</p>
          </div>
        </section>

        <MotivationCard :card="currentMotivation" />
      </aside>

      <main class="content">
        <header class="page-header">
          <div>
            <p class="eyebrow">ReadStack</p>
            <h1>すべての記事</h1>
          </div>
          <SearchFilterBar
            :search="searchDraft"
            :status="store.filters.status"
            :sort="store.filters.sort"
            @update:search="searchDraft = $event"
            @update:status="setStatus($event)"
            @update:sort="setSort($event)"
            @add="modalOpen = true"
          />
        </header>

        <div v-if="store.error" class="error-banner" role="alert" aria-live="assertive">
          <strong>データを読み込めませんでした</strong>
          <span>{{ store.error }}</span>
        </div>

        <section class="article-list" aria-live="polite">
          <div v-if="store.loading" class="loading-state">
            <VProgressCircular indeterminate color="primary" size="42" />
          </div>
          <div v-else-if="store.articles.length === 0" class="empty-state">
            <h2>まだ記事がありません</h2>
            <p>URL を貼り付けて、学びの断片をここに積み上げていきましょう。</p>
            <VBtn color="primary" @click="modalOpen = true">最初の記事を追加</VBtn>
          </div>
          <template v-else>
            <ArticleCard
              v-for="article in store.sortedArticles"
              :key="article.id"
              :article="article"
              :selected="store.selectedArticle?.id === article.id"
              @click="openArticle(article)"
              @toggle-favorite="toggleFavorite(article)"
            />
          </template>
        </section>
      </main>
    </div>

    <ArticleDetail
      v-else
      :article="store.selectedArticle"
      :tags="store.tags"
      @back="showList"
      @save="saveArticle"
      @delete="deleteArticle"
    />

    <ArticleFormModal
      :open="modalOpen"
      :tags="store.tags"
      @close="modalOpen = false"
      @submit="createArticle"
    />
  </VApp>
</template>
