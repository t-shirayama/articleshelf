<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import {
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Circle,
  Heart,
  Library,
} from "lucide-vue-next";
import ArticleCard from "./components/ArticleCard.vue";
import ArticleDetail from "./components/ArticleDetail.vue";
import CalendarView from "./components/CalendarView.vue";
import FilterDialog from "./components/FilterDialog.vue";
import ArticleFormModal from "./components/ArticleFormModal.vue";
import MotivationCard from "./components/MotivationCard.vue";
import SearchFilterBar from "./components/SearchFilterBar.vue";
import { motivationCards } from "./data/motivationCards";
import { ApiRequestError } from "./services/api";
import { useArticlesStore } from "./stores/articles";
import type {
  Article,
  ArticleInput,
  ArticleSort,
  ArticleStatus,
} from "./types";

type PersistedArticleStatus = Exclude<ArticleStatus, "ALL">;

interface StatusUndoState {
  articleId: string;
  status: PersistedArticleStatus;
  readDate: string | null;
}

const store = useArticlesStore();
const modalOpen = ref(false);
const filterDialogOpen = ref(false);
const articleFormError = ref("");
const duplicateArticleId = ref("");
const searchDraft = ref("");
const viewMode = ref<"list" | "calendar" | "detail">("list");
const deleteCandidate = ref<Article | null>(null);
const motivationIndex = ref(randomMotivationIndex());
const statusUndo = ref<StatusUndoState | null>(null);
const statusSnackbarOpen = ref(false);
const statusSnackbarMessage = ref("");
let searchTimer: ReturnType<typeof window.setTimeout> | undefined;

const availableTagNames = computed<string[]>(() =>
  store.tags.map((tag) => tag.name),
);
const currentMotivation = computed(
  () => motivationCards[motivationIndex.value],
);
const pageTitle = computed(() => {
  if (store.filters.favorite) return "お気に入り";
  if (store.filters.status === "UNREAD") return "未読";
  if (store.filters.status === "READ") return "読了";
  if (
    store.filters.tags.length === 1 &&
    store.filters.ratings.length === 0 &&
    !store.filters.createdRange.from &&
    !store.filters.createdRange.to &&
    !store.filters.readRange.from &&
    !store.filters.readRange.to
  ) {
    return store.filters.tags[0];
  }
  if (activeFilterSummary.value.length > 0) return "絞り込み結果";
  return "すべての記事";
});
const activeFilterSummary = computed<string[]>(() => {
  const summary: string[] = [];

  if (store.filters.tags.length > 0) {
    summary.push(
      store.filters.tags.length === 1
        ? `タグ: ${store.filters.tags[0]}`
        : `タグ: ${store.filters.tags.length}件`,
    );
  }

  if (store.filters.ratings.length > 0) {
    const ratingText = store.filters.ratings
      .slice()
      .sort((left, right) => left - right)
      .map((rating) => (rating === 0 ? "未設定" : `${rating}`))
      .join(", ");
    summary.push(`おすすめ度: ${ratingText}`);
  }

  if (store.filters.createdRange.from || store.filters.createdRange.to) {
    summary.push(
      `登録日: ${formatRange(store.filters.createdRange.from, store.filters.createdRange.to)}`,
    );
  }

  if (store.filters.readRange.from || store.filters.readRange.to) {
    summary.push(
      `読了日: ${formatRange(store.filters.readRange.from, store.filters.readRange.to)}`,
    );
  }

  return summary;
});
const activeFilterCount = computed(() => activeFilterSummary.value.length);
const isListMode = computed(() => viewMode.value === "list");
const isAllArticlesActive = computed(
  () =>
    isListMode.value &&
    store.filters.status === "ALL" &&
    activeFilterCount.value === 0 &&
    !store.filters.favorite,
);
const isUnreadActive = computed(
  () => isListMode.value && store.filters.status === "UNREAD",
);
const isReadActive = computed(
  () => isListMode.value && store.filters.status === "READ",
);
const isFavoriteActive = computed(
  () => isListMode.value && store.filters.favorite,
);
const isCalendarActive = computed(() => viewMode.value === "calendar");

watch(searchDraft, (value) => {
  if (searchTimer) window.clearTimeout(searchTimer);
  searchTimer = window.setTimeout(() => {
    store.setSearch(value);
  }, 250);
});

onMounted(async () => {
  await Promise.all([store.fetchArticles(), store.fetchTags()]);
});

async function createArticle(article: ArticleInput): Promise<void> {
  articleFormError.value = "";
  duplicateArticleId.value = "";
  try {
    await store.createArticle(article);
    rotateMotivation();
    modalOpen.value = false;
    viewMode.value = "list";
  } catch (error: unknown) {
    articleFormError.value =
      error instanceof Error ? error.message : "記事を保存できませんでした";
    duplicateArticleId.value =
      error instanceof ApiRequestError ? error.existingArticleId || "" : "";
  }
}

async function saveArticle(article: ArticleInput): Promise<void> {
  await store.updateArticle(article);
}

async function deleteArticle(articleId: string): Promise<void> {
  await store.deleteArticle(articleId);
  showList();
}

function requestDeleteArticle(article: Article): void {
  deleteCandidate.value = article;
}

async function confirmListDelete(): Promise<void> {
  if (!deleteCandidate.value) return;
  const articleId = deleteCandidate.value.id;
  deleteCandidate.value = null;
  await store.deleteArticle(articleId);
}

async function openArticle(article: Article): Promise<void> {
  await store.selectArticle(article);
  rotateMotivation();
  viewMode.value = "detail";
}

async function toggleFavorite(article: Article): Promise<void> {
  await store.toggleFavorite(article);
}

async function toggleArticleStatus(article: Article): Promise<void> {
  const previousStatus = article.status;
  const previousReadDate = article.readDate || null;
  const nextStatus: PersistedArticleStatus =
    article.status === "READ" ? "UNREAD" : "READ";
  const nextReadDate = nextStatus === "READ" ? todayString() : null;
  const updated = await store.updateArticleStatus(
    article,
    nextStatus,
    nextReadDate,
  );
  if (!updated) return;

  statusUndo.value = {
    articleId: article.id,
    status: previousStatus,
    readDate: previousReadDate,
  };
  statusSnackbarMessage.value =
    nextStatus === "READ" ? "読了にしました" : "未読に戻しました";
  statusSnackbarOpen.value = true;
}

async function undoArticleStatus(): Promise<void> {
  if (!statusUndo.value) return;
  const undo = statusUndo.value;
  const article = store.articles.find((item) => item.id === undo.articleId);
  if (!article) return;

  statusSnackbarOpen.value = false;
  statusUndo.value = null;
  await store.updateArticleStatus(article, undo.status, undo.readDate);
}

function showList(): void {
  if (viewMode.value !== "list") rotateMotivation();
  viewMode.value = "list";
}

function showCalendar(): void {
  if (viewMode.value !== "calendar") rotateMotivation();
  viewMode.value = "calendar";
}

function rotateMotivation(): void {
  motivationIndex.value = randomMotivationIndex(motivationIndex.value);
}

function randomMotivationIndex(currentIndex = -1): number {
  if (motivationCards.length <= 1) return 0;

  let nextIndex = currentIndex;
  while (nextIndex === currentIndex) {
    nextIndex = Math.floor(Math.random() * motivationCards.length);
  }
  return nextIndex;
}

function setStatus(status: ArticleStatus): Promise<void> {
  showList();
  rotateMotivation();
  return store.setStatus(status);
}

function setFavoriteOnly(): Promise<void> {
  showList();
  rotateMotivation();
  return store.setFavoriteOnly();
}

function setAllArticles(): Promise<void> {
  showList();
  rotateMotivation();
  return store.setAllArticles();
}

function setSort(sort: ArticleSort): void {
  store.setSort(sort);
}

function openArticleModal(): void {
  articleFormError.value = "";
  duplicateArticleId.value = "";
  modalOpen.value = true;
}

function closeArticleModal(): void {
  articleFormError.value = "";
  duplicateArticleId.value = "";
  modalOpen.value = false;
}

function openFilterDialog(): void {
  filterDialogOpen.value = true;
}

function closeFilterDialog(): void {
  filterDialogOpen.value = false;
}

function applyAdvancedFilters(filters: {
  tags: string[];
  ratings: number[];
  createdRange: { from: string; to: string };
  readRange: { from: string; to: string };
}): void {
  showList();
  rotateMotivation();
  store.setTags(filters.tags);
  store.setRatings(filters.ratings);
  store.setCreatedRange(filters.createdRange);
  store.setReadRange(filters.readRange);
  filterDialogOpen.value = false;
}

async function openDuplicateArticle(articleId: string): Promise<void> {
  articleFormError.value = "";
  duplicateArticleId.value = "";
  modalOpen.value = false;
  await store.selectArticleById(articleId);
  rotateMotivation();
  viewMode.value = "detail";
}

function formatRange(from: string, to: string): string {
  if (from && to) return `${from} - ${to}`;
  if (from) return `${from} 以降`;
  if (to) return `${to} 以前`;
  return "";
}

function todayString(): string {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
</script>

<template>
  <VApp>
    <div v-if="viewMode !== 'detail'" class="app-shell">
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
            :color="isAllArticlesActive ? 'primary' : undefined"
            @click="setAllArticles"
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
            :color="isUnreadActive ? 'primary' : undefined"
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
            :color="isReadActive ? 'primary' : undefined"
            @click="setStatus('READ')"
          >
            <template #prepend>
              <CheckCircle2 :size="18" />
            </template>
            <span>読了</span>
            <strong>{{ store.counts.read }}</strong>
          </VBtn>
          <div class="side-nav-divider" aria-hidden="true" />
          <VBtn
            block
            variant="text"
            :color="isFavoriteActive ? 'primary' : undefined"
            @click="setFavoriteOnly"
          >
            <template #prepend>
              <Heart :size="18" />
            </template>
            <span>お気に入り</span>
          </VBtn>
          <div class="side-nav-divider" aria-hidden="true" />
          <VBtn
            block
            variant="text"
            :color="isCalendarActive ? 'primary' : undefined"
            @click="showCalendar"
          >
            <template #prepend>
              <CalendarDays :size="18" />
            </template>
            <span>カレンダー</span>
          </VBtn>
        </nav>

        <MotivationCard :card="currentMotivation" />
      </aside>

      <main class="content">
        <template v-if="viewMode === 'list'">
          <header class="page-header">
            <div>
              <h1>{{ pageTitle }}</h1>
            </div>
            <SearchFilterBar
              :search="searchDraft"
              :sort="store.filters.sort"
              :filter-summary="activeFilterSummary"
              :active-filter-count="activeFilterCount"
              @update:search="searchDraft = $event"
              @update:sort="setSort($event)"
              @open-filters="openFilterDialog"
              @add="openArticleModal"
            />
          </header>

          <div
            v-if="store.error"
            class="error-banner"
            role="alert"
            aria-live="assertive"
          >
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
              <VBtn
                class="action-button action-button-primary"
                color="primary"
                variant="flat"
                @click="openArticleModal"
                >最初の記事を追加</VBtn
              >
            </div>
            <template v-else>
              <ArticleCard
                v-for="article in store.sortedArticles"
                :key="article.id"
                :article="article"
                :selected="store.selectedArticle?.id === article.id"
                @click="openArticle(article)"
                @delete="requestDeleteArticle(article)"
                @toggle-status="toggleArticleStatus(article)"
                @toggle-favorite="toggleFavorite(article)"
              />
            </template>
          </section>
        </template>

        <CalendarView
          v-else
          :articles="store.articles"
          @open-article="openArticle"
        />
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
      :error="articleFormError"
      :duplicate-article-id="duplicateArticleId"
      @close="closeArticleModal"
      @open-duplicate="openDuplicateArticle"
      @submit="createArticle"
    />

    <FilterDialog
      :open="filterDialogOpen"
      :available-tags="availableTagNames"
      :filters="{
        tags: store.filters.tags,
        ratings: store.filters.ratings,
        createdRange: store.filters.createdRange,
        readRange: store.filters.readRange,
      }"
      @close="closeFilterDialog"
      @apply="applyAdvancedFilters"
    />

    <VDialog
      :model-value="Boolean(deleteCandidate)"
      max-width="420"
      @update:model-value="
        (value) => {
          if (!value) deleteCandidate = null;
        }
      "
    >
      <VCard class="delete-confirm-dialog" title="記事を削除しますか？">
        <VCardText>
          <p>
            「{{
              deleteCandidate?.title
            }}」を削除します。この操作は取り消せません。
          </p>
        </VCardText>
        <VCardActions>
          <VSpacer />
          <VBtn
            class="action-button action-button-secondary"
            variant="outlined"
            @click="deleteCandidate = null"
            >キャンセル</VBtn
          >
          <VBtn
            class="action-button action-button-danger"
            color="error"
            variant="flat"
            @click="confirmListDelete"
            >削除する</VBtn
          >
        </VCardActions>
      </VCard>
    </VDialog>

    <VSnackbar v-model="statusSnackbarOpen" timeout="2000">
      {{ statusSnackbarMessage }}
      <template #actions>
        <VBtn variant="text" @click="undoArticleStatus">元に戻す</VBtn>
      </template>
    </VSnackbar>
  </VApp>
</template>
