<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import {
  BookOpen,
} from "lucide-vue-next";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import AccountSettingsDialog from "../../auth/components/AccountSettingsDialog.vue";
import { useAuthStore } from "../../auth/stores/auth";
import { getCurrentLocale, setCurrentLocale } from "../../../shared/i18n";
import type { SupportedLocale } from "../../../shared/i18n/locales";
import ArticleWorkspaceShell from "../components/ArticleWorkspaceShell.vue";
import ArticleDetail from "../components/ArticleDetail.vue";
import ArticleFormModal from "../components/ArticleFormModal.vue";
import ArticleListView from "../components/ArticleListView.vue";
import CalendarView from "../components/CalendarView.vue";
import DeleteConfirmDialog from "../components/DeleteConfirmDialog.vue";
import FilterDialog from "../components/FilterDialog.vue";
import StatusUndoSnackbar from "../components/StatusUndoSnackbar.vue";
import TagManagementView from "../components/TagManagementView.vue";
import UnsavedChangesDialog from "../components/UnsavedChangesDialog.vue";
import { useArticleActions } from "../composables/useArticleActions";
import { useArticleFilterPresentation } from "../composables/useArticleFilterPresentation";
import { useArticleModalState } from "../composables/useArticleModalState";
import { useArticleSearchDebounce } from "../composables/useArticleSearchDebounce";
import { useMotivationRotation } from "../composables/useMotivationRotation";
import { useStatusUndo } from "../composables/useStatusUndo";
import { useTagActions } from "../composables/useTagActions";
import { useWorkspaceAccountActions } from "../composables/useWorkspaceAccountActions";
import { useWorkspaceNavigation } from "../composables/useWorkspaceNavigation";
import { useArticlesStore } from "../stores/articles";
import type { ArticleSort, ArticleStatus } from "../types";
import type { Article } from "../types";

type DetailReturnView = "list" | "calendar";
type CalendarMode = "created" | "read";

const authStore = useAuthStore();
const store = useArticlesStore();
const { t, locale } = useI18n({ useScope: "global" });
const route = useRoute();
const router = useRouter();
const filterDialogOpen = ref(false);
const accountDialogOpen = ref(false);
const detailFormError = ref("");
const searchDraft = ref("");
const createSnackbarOpen = ref(false);
const createdArticleId = ref("");
const detailReturnView = ref<DetailReturnView>("list");
const calendarVisibleMonthKey = ref(toMonthKey(new Date()));
const calendarMode = ref<CalendarMode>("created");
const mobileDrawerOpen = ref(false);
let removeWorkspaceRouteGuard: (() => void) | null = null;

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
  detailConflictArticleId,
  createArticle,
  saveArticle,
  deleteArticle,
  requestDeleteArticle,
  confirmListDelete,
  openArticle,
  toggleFavorite,
  openDuplicateArticle,
  reloadConflictedArticle,
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
  onCreateSuccess: handleArticleCreateSuccess,
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
const mobileBottomNavigationVisible = computed(
  () =>
    viewMode.value !== "detail" &&
    !mobileDrawerOpen.value &&
    !modalOpen.value &&
    !filterDialogOpen.value &&
    !accountDialogOpen.value &&
    !deleteCandidate.value &&
    !unsavedChangesDialogOpen.value,
);

const { cancelSearch } = useArticleSearchDebounce(searchDraft, async (value) => {
  store.setSearch(value);
  await store.fetchArticles();
});

onMounted(async () => {
  window.addEventListener("beforeunload", handleBeforeUnload);
  removeWorkspaceRouteGuard = router.beforeEach((to, from) => {
    if (
      to.fullPath === from.fullPath ||
      !isWorkspaceRoute(to.path) ||
      viewMode.value !== "detail" ||
      !detailHasUnsavedChanges.value
    ) {
      return true;
    }

    requestNavigation(() => {
      void router.push(to.fullPath);
    });
    return false;
  });
  await loadInitialData();
  await applyWorkspaceRoute();
});

watch(() => route.path, () => {
  void applyWorkspaceRoute();
});

onBeforeUnmount(() => {
  window.removeEventListener("beforeunload", handleBeforeUnload);
  removeWorkspaceRouteGuard?.();
  removeWorkspaceRouteGuard = null;
});

function showDetailReturnView(): void {
  mobileDrawerOpen.value = false;
  requestNavigation(navigateToDetailReturnView);
}

function showCalendar(): void {
  mobileDrawerOpen.value = false;
  requestNavigation(() => {
    void router.push("/calendar");
    navigateToCalendar();
  });
}

function showTags(): void {
  mobileDrawerOpen.value = false;
  requestNavigation(() => {
    void router.push("/tags");
    navigateToTags();
  });
}

function setStatus(status: ArticleStatus): void {
  mobileDrawerOpen.value = false;
  requestNavigation(() => {
    void router.push("/articles");
    navigateToList();
    rotateMotivation();
    store.setStatus(status);
    void store.fetchArticles();
  });
}

function setFavoriteOnly(): void {
  mobileDrawerOpen.value = false;
  requestNavigation(() => {
    void router.push("/articles");
    navigateToList();
    rotateMotivation();
    store.setFavoriteOnly();
    void store.fetchArticles();
  });
}

function setAllArticles(): void {
  mobileDrawerOpen.value = false;
  requestNavigation(() => {
    void router.push("/articles");
    navigateToList();
    rotateMotivation();
    store.setAllArticles();
    void store.fetchArticles();
  });
}

function setSort(sort: ArticleSort): void {
  store.setSort(sort);
  void store.fetchArticles();
}

function closeArticleModal(): void {
  closeArticleModalState(isCreatingArticle.value);
}

function openArticleModalFromMobile(): void {
  mobileDrawerOpen.value = false;
  openArticleModal();
}

function changeLocale(value: unknown): void {
  const nextLocale: SupportedLocale = value === "ja" ? "ja" : "en";
  setCurrentLocale(nextLocale);
  locale.value = getCurrentLocale();
}

async function loadInitialData(): Promise<void> {
  await Promise.all([store.fetchArticles(), store.fetchArticleSnapshot(), store.fetchTags()]);
}

async function applyWorkspaceRoute(): Promise<void> {
  if (route.path === "/calendar") {
    navigateToCalendar();
    return;
  }
  if (route.path === "/tags") {
    navigateToTags();
    return;
  }
  if (route.path === "/settings") {
    navigateWorkspaceToList();
    accountDialogOpen.value = true;
    return;
  }
  if (typeof route.params.id === "string") {
    if (viewMode.value !== "calendar" && detailReturnView.value !== "calendar") {
      detailReturnView.value = "list";
    }
    await openDuplicateArticle(route.params.id);
    return;
  }
  navigateWorkspaceToList();
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
    void store.fetchArticles();
  });
}

function navigateToList(): void {
  detailFormError.value = "";
  if (route.path !== "/articles") void router.push("/articles");
  navigateWorkspaceToList();
}

function navigateToDetailReturnView(): void {
  detailFormError.value = "";
  if (detailReturnView.value === "calendar") {
    void router.push("/calendar");
    navigateToCalendar();
    return;
  }

  void router.push("/articles");
  navigateWorkspaceToList();
}

function currentDetailReturnView(): DetailReturnView {
  return viewMode.value === "calendar" ? "calendar" : "list";
}

function openArticleFromList(article: Article): Promise<void> {
  detailReturnView.value = "list";
  void router.push(`/articles/${article.id}`);
  return openArticle(article);
}

function openArticleFromCalendar(article: Article): Promise<void> {
  detailReturnView.value = "calendar";
  void router.push(`/articles/${article.id}`);
  return openArticle(article);
}

function openDuplicateArticleFromCurrentView(articleId: string): Promise<void> {
  detailReturnView.value = currentDetailReturnView();
  return openDuplicateArticle(articleId);
}

function handleArticleCreateSuccess(article: Article): void {
  createdArticleId.value = article.id;
  createSnackbarOpen.value = true;
}

function editCreatedArticle(): void {
  if (!createdArticleId.value) return;
  createSnackbarOpen.value = false;
  detailReturnView.value = "list";
  void router.push(`/articles/${createdArticleId.value}`);
  void openDuplicateArticle(createdArticleId.value);
}

function continueCreatingArticle(): void {
  createSnackbarOpen.value = false;
  openArticleModal();
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

function isWorkspaceRoute(path: string): boolean {
  return (
    path === "/articles" ||
    path.startsWith("/articles/") ||
    path === "/calendar" ||
    path === "/tags" ||
    path === "/settings"
  );
}

const {
  accountDialogError,
  logout,
  changePassword,
  logoutAll,
  deleteAccount,
  openAccountSettings,
} = useWorkspaceAccountActions({
  authStore,
  store,
  cancelSearch,
  resetNavigation,
  accountDialogOpen,
  mobileDrawerOpen,
});
</script>

<template>
  <ArticleWorkspaceShell
    v-if="viewMode !== 'detail'"
    :counts="store.counts"
    :current-motivation="currentMotivation"
    :is-all-articles-active="isAllArticlesActive"
    :is-unread-active="isUnreadActive"
    :is-read-active="isReadActive"
    :is-favorite-active="isFavoriteActive"
    :is-calendar-active="isCalendarActive"
    :is-tags-active="isTagsActive"
    :user-name="authStore.user?.displayName || authStore.user?.username || ''"
    :drawer-open="mobileDrawerOpen"
    :bottom-navigation-visible="mobileBottomNavigationVisible"
    :current-locale="getCurrentLocale()"
    @update:drawer-open="mobileDrawerOpen = $event"
    @all-articles="setAllArticles"
    @status="setStatus"
    @favorite-only="setFavoriteOnly"
    @calendar="showCalendar"
    @tags="showTags"
    @account="openAccountSettings"
    @logout="logout"
    @change-locale="changeLocale"
    @add-article="openArticleModalFromMobile"
  >
    <ArticleListView
      v-if="viewMode === 'list'"
      :title="pageTitle"
      :search="searchDraft"
      :sort="store.filters.sort"
      :filter-summary="activeFilterSummary"
      :active-filter-count="activeFilterCount"
      :error="store.error"
      :loading="store.loading"
      :articles="store.articles"
      :current-page="store.currentPage"
      :has-next-page="store.hasNextPage"
      :has-previous-page="store.hasPreviousPage"
      :selected-article-id="store.selectedArticle?.id"
      @update:search="searchDraft = $event"
      @update:sort="setSort"
      @open-filters="filterDialogOpen = true"
      @add="openArticleModalFromMobile"
      @open-article="openArticleFromList"
      @delete-article="requestDeleteArticle"
      @toggle-status="toggleArticleStatus"
      @toggle-favorite="toggleFavorite"
      @previous-page="store.goToPreviousPage()"
      @next-page="store.goToNextPage()"
      @retry="retryInitialLoad"
    />

    <CalendarView
      v-else-if="viewMode === 'calendar'"
      v-model:visible-month-key="calendarVisibleMonthKey"
      v-model:mode="calendarMode"
      :articles="store.articleSnapshot"
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
  </ArticleWorkspaceShell>

  <ArticleDetail
    v-else
    :article="store.selectedArticle"
    :tags="store.tags"
    :saving="isSavingDetail"
    :deleting="isDeletingArticle"
    :error="detailFormError"
    :error-action-label="detailConflictArticleId ? t('detail.reloadLatest') : ''"
    @update:dirty="detailHasUnsavedChanges = $event"
    @back="showDetailReturnView"
    @save="saveArticle"
    @delete="deleteArticle"
    @error-action="reloadConflictedArticle"
  />

  <ArticleFormModal
    :open="modalOpen"
    :tags="store.tags"
    :error="articleFormError"
    :duplicate-article-id="duplicateArticleId"
    :saving="isCreatingArticle"
    @close="closeArticleModal"
    @open-duplicate="openDuplicateArticleFromCurrentView"
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

  <StatusUndoSnackbar
    :open="createSnackbarOpen"
    :message="t('articles.saveDone')"
    :primary-action-label="t('articles.editSavedDetail')"
    :secondary-action-label="t('articles.saveAnother')"
    @update:open="createSnackbarOpen = $event"
    @primary-action="editCreatedArticle"
    @secondary-action="continueCreatingArticle"
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
