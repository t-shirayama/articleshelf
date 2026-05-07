<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { ApiRequestError } from "../../../shared/api/client";
import { useAuthStore } from "../../auth/stores/auth";
import ArticleDetail from "../components/ArticleDetail.vue";
import ArticleFormModal from "../components/ArticleFormModal.vue";
import ArticleListView from "../components/ArticleListView.vue";
import AppSidebar from "../components/AppSidebar.vue";
import CalendarView from "../components/CalendarView.vue";
import DeleteConfirmDialog from "../components/DeleteConfirmDialog.vue";
import FilterDialog from "../components/FilterDialog.vue";
import StatusUndoSnackbar from "../components/StatusUndoSnackbar.vue";
import TagManagementView from "../components/TagManagementView.vue";
import UnsavedChangesDialog from "../components/UnsavedChangesDialog.vue";
import { useArticleFilterPresentation } from "../composables/useArticleFilterPresentation";
import { useMotivationRotation } from "../composables/useMotivationRotation";
import { useArticlesStore } from "../stores/articles";
import type {
  Article,
  ArticleInput,
  ArticleSort,
  ArticleStatus,
} from "../types";

type PersistedArticleStatus = Exclude<ArticleStatus, "ALL">;
type ViewMode = "list" | "calendar" | "detail" | "tags";

interface StatusUndoState {
  articleId: string;
  status: PersistedArticleStatus;
  readDate: string | null;
}

const authStore = useAuthStore();
const store = useArticlesStore();
const modalOpen = ref(false);
const filterDialogOpen = ref(false);
const articleFormError = ref("");
const detailFormError = ref("");
const duplicateArticleId = ref("");
const searchDraft = ref("");
const viewMode = ref<ViewMode>("list");
const deleteCandidate = ref<Article | null>(null);
const statusUndo = ref<StatusUndoState | null>(null);
const statusSnackbarOpen = ref(false);
const statusSnackbarMessage = ref("");
const detailHasUnsavedChanges = ref(false);
const unsavedChangesDialogOpen = ref(false);
const pendingNavigation = ref<(() => void) | null>(null);
const isCreatingArticle = ref(false);
const isSavingDetail = ref(false);
const isDeletingArticle = ref(false);
const isSavingTag = ref(false);
let searchTimer: ReturnType<typeof window.setTimeout> | undefined;

const availableTagNames = computed<string[]>(() =>
  store.tags.map((tag) => tag.name),
);
const filters = computed(() => store.filters);
const { activeFilterCount, activeFilterSummary, pageTitle } =
  useArticleFilterPresentation(filters);
const { currentMotivation, rotateMotivation } = useMotivationRotation();
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
const isTagsActive = computed(() => viewMode.value === "tags");

watch(searchDraft, (value) => {
  if (searchTimer) window.clearTimeout(searchTimer);
  searchTimer = window.setTimeout(() => {
    store.setSearch(value);
  }, 250);
});

onMounted(async () => {
  window.addEventListener("beforeunload", handleBeforeUnload);
  await loadInitialData();
});

onBeforeUnmount(() => {
  window.removeEventListener("beforeunload", handleBeforeUnload);
});

async function createArticle(article: ArticleInput): Promise<void> {
  if (isCreatingArticle.value) return;
  articleFormError.value = "";
  duplicateArticleId.value = "";
  isCreatingArticle.value = true;
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
  } finally {
    isCreatingArticle.value = false;
  }
}

async function saveArticle(article: ArticleInput): Promise<void> {
  if (isSavingDetail.value) return;
  detailFormError.value = "";
  isSavingDetail.value = true;
  try {
    await store.updateArticle(article);
  } catch (error: unknown) {
    detailFormError.value =
      error instanceof Error ? error.message : "記事を保存できませんでした";
  } finally {
    isSavingDetail.value = false;
  }
}

async function logout(): Promise<void> {
  await authStore.logout();
  store.resetState();
  detailHasUnsavedChanges.value = false;
  viewMode.value = "list";
}

async function deleteArticle(articleId: string): Promise<void> {
  if (isDeletingArticle.value) return;
  detailFormError.value = "";
  isDeletingArticle.value = true;
  try {
    await store.deleteArticle(articleId);
    detailHasUnsavedChanges.value = false;
    navigateToList();
  } catch (error: unknown) {
    detailFormError.value =
      error instanceof Error ? error.message : "記事を削除できませんでした";
  } finally {
    isDeletingArticle.value = false;
  }
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
  detailFormError.value = "";
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
    nextStatus === "READ" ? "既読にしました" : "未読に戻しました";
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
  requestNavigation(navigateToList);
}

function showCalendar(): void {
  requestNavigation(navigateToCalendar);
}

function showTags(): void {
  requestNavigation(navigateToTags);
}

function setStatus(status: ArticleStatus): void {
  requestNavigation(() => {
    navigateToList();
    rotateMotivation();
    void store.setStatus(status);
  });
}

function setFavoriteOnly(): void {
  requestNavigation(() => {
    navigateToList();
    rotateMotivation();
    void store.setFavoriteOnly();
  });
}

function setAllArticles(): void {
  requestNavigation(() => {
    navigateToList();
    rotateMotivation();
    void store.setAllArticles();
  });
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
  if (isCreatingArticle.value) return;
  articleFormError.value = "";
  duplicateArticleId.value = "";
  modalOpen.value = false;
}

async function loadInitialData(): Promise<void> {
  await Promise.all([store.fetchArticles(), store.fetchTags()]);
}

async function retryInitialLoad(): Promise<void> {
  await loadInitialData();
}

async function renameTag(id: string, name: string): Promise<void> {
  if (isSavingTag.value) return;
  isSavingTag.value = true;
  store.error = "";
  try {
    await store.renameTag(id, name);
  } catch (error: unknown) {
    store.error = error instanceof Error ? error.message : "タグ名を変更できませんでした";
  } finally {
    isSavingTag.value = false;
  }
}

async function mergeTag(sourceId: string, targetId: string): Promise<void> {
  if (isSavingTag.value) return;
  isSavingTag.value = true;
  store.error = "";
  try {
    await store.mergeTag(sourceId, targetId);
  } catch (error: unknown) {
    store.error = error instanceof Error ? error.message : "タグを統合できませんでした";
  } finally {
    isSavingTag.value = false;
  }
}

async function deleteTag(id: string): Promise<void> {
  if (isSavingTag.value) return;
  isSavingTag.value = true;
  store.error = "";
  try {
    await store.deleteTag(id);
  } catch (error: unknown) {
    store.error = error instanceof Error ? error.message : "タグを削除できませんでした";
  } finally {
    isSavingTag.value = false;
  }
}

function openTagArticles(tagName: string): void {
  requestNavigation(() => {
    navigateToList();
    searchDraft.value = "";
    store.setSearch("");
    void store.setAllArticles();
    store.setTags([tagName]);
  });
}

function applyAdvancedFilters(filters: {
  tags: string[];
  ratings: number[];
  createdRange: { from: string; to: string };
  readRange: { from: string; to: string };
}): void {
  requestNavigation(() => {
    navigateToList();
    rotateMotivation();
    store.setTags(filters.tags);
    store.setRatings(filters.ratings);
    store.setCreatedRange(filters.createdRange);
    store.setReadRange(filters.readRange);
    filterDialogOpen.value = false;
  });
}

async function openDuplicateArticle(articleId: string): Promise<void> {
  articleFormError.value = "";
  duplicateArticleId.value = "";
  modalOpen.value = false;
  await store.selectArticleById(articleId);
  rotateMotivation();
  viewMode.value = "detail";
}

function todayString(): string {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function requestNavigation(action: () => void): void {
  if (viewMode.value === "detail" && detailHasUnsavedChanges.value) {
    pendingNavigation.value = action;
    unsavedChangesDialogOpen.value = true;
    return;
  }

  action();
}

function navigateToList(): void {
  if (viewMode.value !== "list") rotateMotivation();
  detailFormError.value = "";
  viewMode.value = "list";
}

function navigateToCalendar(): void {
  if (viewMode.value !== "calendar") rotateMotivation();
  viewMode.value = "calendar";
}

function navigateToTags(): void {
  if (viewMode.value !== "tags") rotateMotivation();
  viewMode.value = "tags";
}

function cancelPendingNavigation(): void {
  pendingNavigation.value = null;
  unsavedChangesDialogOpen.value = false;
}

function confirmPendingNavigation(): void {
  const action = pendingNavigation.value;
  pendingNavigation.value = null;
  unsavedChangesDialogOpen.value = false;
  detailHasUnsavedChanges.value = false;
  action?.();
}

function handleBeforeUnload(event: BeforeUnloadEvent): void {
  if (!detailHasUnsavedChanges.value) return;
  event.preventDefault();
  event.returnValue = "";
}
</script>

<template>
  <div v-if="viewMode !== 'detail'" class="app-shell">
    <AppSidebar
      :counts="store.counts"
      :current-motivation="currentMotivation"
      :is-all-articles-active="isAllArticlesActive"
      :is-unread-active="isUnreadActive"
      :is-read-active="isReadActive"
      :is-favorite-active="isFavoriteActive"
      :is-calendar-active="isCalendarActive"
      :is-tags-active="isTagsActive"
      :user-name="authStore.user?.displayName || authStore.user?.email || ''"
      @all-articles="setAllArticles"
      @status="setStatus"
      @favorite-only="setFavoriteOnly"
      @calendar="showCalendar"
      @tags="showTags"
      @logout="logout"
    />

    <main class="content">
      <ArticleListView
        v-if="viewMode === 'list'"
        :title="pageTitle"
        :search="searchDraft"
        :sort="store.filters.sort"
        :filter-summary="activeFilterSummary"
        :active-filter-count="activeFilterCount"
        :error="store.error"
        :loading="store.loading"
        :articles="store.sortedArticles"
        :selected-article-id="store.selectedArticle?.id"
        @update:search="searchDraft = $event"
        @update:sort="setSort"
        @open-filters="filterDialogOpen = true"
        @add="openArticleModal"
        @open-article="openArticle"
        @delete-article="requestDeleteArticle"
        @toggle-status="toggleArticleStatus"
        @toggle-favorite="toggleFavorite"
        @retry="retryInitialLoad"
      />

      <CalendarView
        v-else-if="viewMode === 'calendar'"
        :articles="store.articles"
        @open-article="openArticle"
      />

      <TagManagementView
        v-else
        :tags="store.tags"
        :error="store.error"
        :loading="store.loading"
        :saving="isSavingTag"
        @rename-tag="renameTag"
        @merge-tag="mergeTag"
        @delete-tag="deleteTag"
        @open-tag-articles="openTagArticles"
        @add-article="openArticleModal"
        @retry="retryInitialLoad"
      />
    </main>
  </div>

  <ArticleDetail
    v-else
    :article="store.selectedArticle"
    :tags="store.tags"
    :saving="isSavingDetail"
    :deleting="isDeletingArticle"
    :error="detailFormError"
    @update:dirty="detailHasUnsavedChanges = $event"
    @back="showList"
    @save="saveArticle"
    @delete="deleteArticle"
  />

  <ArticleFormModal
    :open="modalOpen"
    :tags="store.tags"
    :error="articleFormError"
    :duplicate-article-id="duplicateArticleId"
    :saving="isCreatingArticle"
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
    @close="filterDialogOpen = false"
    @apply="applyAdvancedFilters"
  />

  <DeleteConfirmDialog
    :article="deleteCandidate"
    @cancel="deleteCandidate = null"
    @confirm="confirmListDelete"
  />

  <StatusUndoSnackbar
    :open="statusSnackbarOpen"
    :message="statusSnackbarMessage"
    @update:open="statusSnackbarOpen = $event"
    @undo="undoArticleStatus"
  />

  <UnsavedChangesDialog
    :open="unsavedChangesDialogOpen"
    @cancel="cancelPendingNavigation"
    @confirm="confirmPendingNavigation"
  />
</template>
