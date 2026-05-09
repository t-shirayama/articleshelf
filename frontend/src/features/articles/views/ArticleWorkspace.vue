<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import AccountSettingsDialog from "../../auth/components/AccountSettingsDialog.vue";
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
import { useArticleActions } from "../composables/useArticleActions";
import { useArticleFilterPresentation } from "../composables/useArticleFilterPresentation";
import { useArticleModalState } from "../composables/useArticleModalState";
import { useMotivationRotation } from "../composables/useMotivationRotation";
import { useStatusUndo } from "../composables/useStatusUndo";
import { useTagActions } from "../composables/useTagActions";
import { useWorkspaceNavigation } from "../composables/useWorkspaceNavigation";
import { useArticlesStore } from "../stores/articles";
import type { ArticleSort, ArticleStatus } from "../types";
import type { Article } from "../types";

type DetailReturnView = "list" | "calendar";
type CalendarMode = "created" | "read";

const authStore = useAuthStore();
const store = useArticlesStore();
const { t } = useI18n();
const filterDialogOpen = ref(false);
const accountDialogOpen = ref(false);
const accountDialogError = ref("");
const detailFormError = ref("");
const searchDraft = ref("");
const detailReturnView = ref<DetailReturnView>("list");
const calendarVisibleMonthKey = ref(toMonthKey(new Date()));
const calendarMode = ref<CalendarMode>("created");
let searchTimer: ReturnType<typeof window.setTimeout> | undefined;

const availableTagNames = computed<string[]>(() =>
  store.tags.map((tag) => tag.name),
);
const filters = computed(() => store.filters);
const { activeFilterCount, activeFilterSummary, pageTitle } =
  useArticleFilterPresentation(filters);
const { currentMotivation, rotateMotivation } = useMotivationRotation();
const {
  viewMode,
  detailHasUnsavedChanges,
  unsavedChangesDialogOpen,
  isListMode,
  isCalendarActive,
  isTagsActive,
  requestNavigation,
  navigateToList: navigateWorkspaceToList,
  navigateToCalendar,
  navigateToTags,
  cancelPendingNavigation,
  confirmPendingNavigation,
  resetNavigation,
} = useWorkspaceNavigation(rotateMotivation);
const {
  modalOpen,
  articleFormError,
  duplicateArticleId,
  openArticleModal,
  closeArticleModal: closeArticleModalState,
  closeForDuplicateOpen,
} = useArticleModalState();
const {
  deleteCandidate,
  isCreatingArticle,
  isSavingDetail,
  isDeletingArticle,
  createArticle,
  saveArticle,
  deleteArticle,
  requestDeleteArticle,
  confirmListDelete,
  openArticle,
  toggleFavorite,
  openDuplicateArticle,
} = useArticleActions({
  store,
  t,
  viewMode,
  detailHasUnsavedChanges,
  articleFormError,
  detailFormError,
  duplicateArticleId,
  modalOpen,
  rotateMotivation,
  navigateToList,
  closeForDuplicateOpen,
});
const {
  statusSnackbarOpen,
  statusSnackbarMessage,
  toggleArticleStatus,
  undoArticleStatus,
} = useStatusUndo(store, t);
const {
  isSavingTag,
  renameTag,
  addTag,
  mergeTag,
  deleteTag,
  openTagArticles,
} = useTagActions({
  store,
  t,
  searchDraft,
  navigateToList,
  requestNavigation,
});
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

async function logout(): Promise<void> {
  await authStore.logout();
  resetUserScopedState();
}

async function changePassword(input: { currentPassword: string; newPassword: string }): Promise<void> {
  accountDialogError.value = "";
  try {
    await authStore.changePassword(input);
    resetUserScopedState();
    accountDialogOpen.value = false;
  } catch {
    accountDialogError.value = authStore.error;
  }
}

async function logoutAll(): Promise<void> {
  accountDialogError.value = "";
  try {
    await authStore.logoutAll();
    resetUserScopedState();
    accountDialogOpen.value = false;
  } catch {
    accountDialogError.value = authStore.error;
  }
}

async function deleteAccount(input: { currentPassword: string }): Promise<void> {
  accountDialogError.value = "";
  try {
    await authStore.deleteAccount(input);
    resetUserScopedState();
    accountDialogOpen.value = false;
  } catch {
    accountDialogError.value = authStore.error;
  }
}

function resetUserScopedState(): void {
  store.resetState();
  resetNavigation();
}

function showList(): void {
  requestNavigation(navigateToList);
}

function showDetailReturnView(): void {
  requestNavigation(navigateToDetailReturnView);
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

function closeArticleModal(): void {
  closeArticleModalState(isCreatingArticle.value);
}

async function loadInitialData(): Promise<void> {
  await Promise.all([store.fetchArticles(), store.fetchTags()]);
}

async function retryInitialLoad(): Promise<void> {
  await loadInitialData();
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

function navigateToList(): void {
  detailFormError.value = "";
  navigateWorkspaceToList();
}

function navigateToDetailReturnView(): void {
  detailFormError.value = "";
  if (detailReturnView.value === "calendar") {
    navigateToCalendar();
    return;
  }

  navigateWorkspaceToList();
}

function openArticleFromList(article: Article): Promise<void> {
  detailReturnView.value = "list";
  return openArticle(article);
}

function openArticleFromCalendar(article: Article): Promise<void> {
  detailReturnView.value = "calendar";
  return openArticle(article);
}

function toMonthKey(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  return `${year}-${month}`;
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
      :user-name="authStore.user?.displayName || authStore.user?.username || ''"
      @all-articles="setAllArticles"
      @status="setStatus"
      @favorite-only="setFavoriteOnly"
      @calendar="showCalendar"
      @tags="showTags"
      @account="accountDialogOpen = true"
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
        @open-article="openArticleFromList"
        @delete-article="requestDeleteArticle"
        @toggle-status="toggleArticleStatus"
        @toggle-favorite="toggleFavorite"
        @retry="retryInitialLoad"
      />

      <CalendarView
        v-else-if="viewMode === 'calendar'"
        :articles="store.articles"
        v-model:visible-month-key="calendarVisibleMonthKey"
        v-model:mode="calendarMode"
        @open-article="openArticleFromCalendar"
      />

      <TagManagementView
        v-else
        :tags="store.tags"
        :error="store.error"
        :loading="store.loading"
        :saving="isSavingTag"
        @add-tag="addTag"
        @rename-tag="renameTag"
        @merge-tag="mergeTag"
        @delete-tag="deleteTag"
        @open-tag-articles="openTagArticles"
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
    @back="showDetailReturnView"
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

  <AccountSettingsDialog
    :open="accountDialogOpen"
    :loading="authStore.loading"
    :error="accountDialogError || authStore.error"
    @close="accountDialogOpen = false"
    @change-password="changePassword"
    @logout-all="logoutAll"
    @delete-account="deleteAccount"
  />
</template>
