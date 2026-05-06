<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { BookOpen, CheckCircle2, Circle, Heart, Library, Tag } from 'lucide-vue-next'
import ArticleCard from './components/ArticleCard.vue'
import ArticleDetail from './components/ArticleDetail.vue'
import ArticleFormModal from './components/ArticleFormModal.vue'
import SearchFilterBar from './components/SearchFilterBar.vue'
import { useArticlesStore } from './stores/articles'

const store = useArticlesStore()
const modalOpen = ref(false)
const searchDraft = ref('')
const viewMode = ref('list')
let searchTimer = 0

const visibleTags = computed(() => store.tags.slice(0, 8))

watch(searchDraft, (value) => {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    store.setSearch(value)
  }, 250)
})

onMounted(async () => {
  await Promise.all([store.fetchArticles(), store.fetchTags()])
})

async function createArticle(article) {
  await store.createArticle(article)
  modalOpen.value = false
  viewMode.value = 'detail'
}

async function saveArticle(article) {
  await store.updateArticle(article)
}

async function deleteArticle(articleId) {
  await store.deleteArticle(articleId)
  viewMode.value = 'list'
}

async function openArticle(article) {
  await store.selectArticle(article)
  viewMode.value = 'detail'
}

function showList() {
  viewMode.value = 'list'
}

function setStatus(status) {
  showList()
  return store.setStatus(status)
}

function setTag(tag) {
  showList()
  return store.setTag(tag)
}

function setFavoriteOnly() {
  showList()
  return store.setFavoriteOnly()
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
        </section>
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
            @update:search="searchDraft = $event"
            @update:status="setStatus($event)"
            @add="modalOpen = true"
          />
        </header>

        <VAlert v-if="store.error" type="error" class="error-banner" variant="tonal">
          {{ store.error }}
        </VAlert>

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
              v-for="article in store.articles"
              :key="article.id"
              :article="article"
              :selected="store.selectedArticle?.id === article.id"
              @click="openArticle(article)"
            />
          </template>
        </section>
      </main>
    </div>

    <ArticleDetail
      v-else
      :article="store.selectedArticle"
      @back="showList"
      @save="saveArticle"
      @delete="deleteArticle"
    />

    <ArticleFormModal
      :open="modalOpen"
      @close="modalOpen = false"
      @submit="createArticle"
    />
  </VApp>
</template>
