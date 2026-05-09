<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import {
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Circle,
  Heart,
  Library,
  LogOut,
  Menu,
  Plus,
  Tags,
  UserCog,
} from "lucide-vue-next";
import { useI18n } from "vue-i18n";
import AccountSettingsDialog from "../../auth/components/AccountSettingsDialog.vue";
import { useAuthStore } from "../../auth/stores/auth";
import { getCurrentLocale, setCurrentLocale } from "../../../shared/i18n";
import type { SupportedLocale } from "../../../shared/i18n/locales";
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
const { t, locale } = useI18n({ useScope: "global" });
const filterDialogOpen = ref(false);
const accountDialogOpen = ref(false);
const accountDialogError = ref("");
const detailFormError = ref("");
const searchDraft = ref("");
const detailReturnView = ref<DetailReturnView>("list");
const calendarVisibleMonthKey = ref(toMonthKey(new Date()));
const calendarMode = ref<CalendarMode>("created");
const mobileDrawerOpen = ref(false);
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
  mobileDrawerOpen.value = false;
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
  mobileDrawerOpen.value = false;
  requestNavigation(navigateToList);
}

function showDetailReturnView(): void {
  mobileDrawerOpen.value = false;
  requestNavigation(navigateToDetailReturnView);
}

function showCalendar(): void {
  mobileDrawerOpen.value = false;
  requestNavigation(navigateToCalendar);
}

function showTags(): void {
  mobileDrawerOpen.value = false;
  requestNavigation(navigateToTags);
}

function setStatus(status: ArticleStatus): void {
  mobileDrawerOpen.value = false;
  requestNavigation(() => {
    navigateToList();
    rotateMotivation();
    void store.setStatus(status);
  });
}

function setFavoriteOnly(): void {
  mobileDrawerOpen.value = false;
  requestNavigation(() => {
    navigateToList();
    rotateMotivation();
    void store.setFavoriteOnly();
  });
}

function setAllArticles(): void {
  mobileDrawerOpen.value = false;
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

function openArticleModalFromMobile(): void {
  mobileDrawerOpen.value = false;
  openArticleModal();
}

function openAccountSettings(): void {
  mobileDrawerOpen.value = false;
  accountDialogOpen.value = true;
}

function changeLocale(value: unknown): void {
  const nextLocale: SupportedLocale = value === "ja" ? "ja" : "en";
  setCurrentLocale(nextLocale);
  locale.value = getCurrentLocale();
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

function currentDetailReturnView(): DetailReturnView {
  return viewMode.value === "calendar" ? "calendar" : "list";
}

function openArticleFromList(article: Article): Promise<void> {
  detailReturnView.value = "list";
  return openArticle(article);
}

function openArticleFromCalendar(article: Article): Promise<void> {
  detailReturnView.value = "calendar";
  return openArticle(article);
}

function openDuplicateArticleFromCurrentView(articleId: string): Promise<void> {
  detailReturnView.value = currentDetailReturnView();
  return openDuplicateArticle(articleId);
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
    <header class="mobile-app-header">
      <VBtn
        class="mobile-menu-button"
        variant="text"
        :aria-label="t('mobile.openMenu')"
        @click="mobileDrawerOpen = true"
      >
        <Menu :size="22" />
      </VBtn>
      <div class="brand mobile-brand">
        <div class="brand-mark">
          <BookOpen :size="20" />
        </div>
        <div class="brand-copy">
          <strong>{{ t("common.appName") }}</strong>
          <span>{{ t("common.appTagline") }}</span>
        </div>
      </div>
    </header>

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
      @account="openAccountSettings"
      @logout="logout"
    />

    <VNavigationDrawer
      v-model="mobileDrawerOpen"
      class="mobile-navigation-drawer"
      temporary
      width="304"
      :aria-label="t('mobile.menu')"
    >
      <div class="mobile-drawer-inner">
        <div class="brand">
          <div class="brand-mark">
            <BookOpen :size="22" />
          </div>
          <div class="brand-copy">
            <strong>{{ t("common.appName") }}</strong>
            <span>{{ t("common.appTagline") }}</span>
          </div>
        </div>

        <nav class="side-nav">
          <VBtn
            class="side-nav-item"
            :class="{ 'is-active': isAllArticlesActive }"
            block
            variant="text"
            :color="isAllArticlesActive ? 'primary' : undefined"
            @click="setAllArticles"
          >
            <template #prepend>
              <Library :size="18" />
            </template>
            <span>{{ t("nav.allArticles") }}</span>
            <strong>{{ store.counts.all }}</strong>
          </VBtn>
          <VBtn
            class="side-nav-item"
            :class="{ 'is-active': isUnreadActive }"
            block
            variant="text"
            :color="isUnreadActive ? 'primary' : undefined"
            @click="setStatus('UNREAD')"
          >
            <template #prepend>
              <Circle :size="18" />
            </template>
            <span>{{ t("nav.unread") }}</span>
            <strong>{{ store.counts.unread }}</strong>
          </VBtn>
          <VBtn
            class="side-nav-item"
            :class="{ 'is-active': isReadActive }"
            block
            variant="text"
            :color="isReadActive ? 'primary' : undefined"
            @click="setStatus('READ')"
          >
            <template #prepend>
              <CheckCircle2 :size="18" />
            </template>
            <span>{{ t("nav.read") }}</span>
            <strong>{{ store.counts.read }}</strong>
          </VBtn>
          <div class="side-nav-divider" aria-hidden="true" />
          <VBtn
            class="side-nav-item"
            :class="{ 'is-active': isFavoriteActive }"
            block
            variant="text"
            :color="isFavoriteActive ? 'primary' : undefined"
            @click="setFavoriteOnly"
          >
            <template #prepend>
              <Heart :size="18" />
            </template>
            <span>{{ t("nav.favorite") }}</span>
          </VBtn>
          <div class="side-nav-divider" aria-hidden="true" />
          <VBtn
            class="side-nav-item"
            :class="{ 'is-active': isCalendarActive }"
            block
            variant="text"
            :color="isCalendarActive ? 'primary' : undefined"
            @click="showCalendar"
          >
            <template #prepend>
              <CalendarDays :size="18" />
            </template>
            <span>{{ t("nav.calendar") }}</span>
          </VBtn>
          <VBtn
            class="side-nav-item"
            :class="{ 'is-active': isTagsActive }"
            block
            variant="text"
            :color="isTagsActive ? 'primary' : undefined"
            @click="showTags"
          >
            <template #prepend>
              <Tags :size="18" />
            </template>
            <span>{{ t("nav.tagManagement") }}</span>
          </VBtn>
        </nav>

        <div class="sidebar-account mobile-drawer-account">
          <span>{{ authStore.user?.displayName || authStore.user?.username || "" }}</span>
          <VBtnToggle
            class="sidebar-language-toggle"
            :model-value="getCurrentLocale()"
            mandatory
            divided
            density="comfortable"
            color="primary"
            :aria-label="t('locale.label')"
            @update:model-value="changeLocale"
          >
            <VBtn value="ja">{{ t("locale.ja") }}</VBtn>
            <VBtn value="en">{{ t("locale.en") }}</VBtn>
          </VBtnToggle>
          <VBtn
            block
            variant="outlined"
            color="primary"
            class="sidebar-account-button"
            @click="openAccountSettings"
          >
            <template #prepend>
              <UserCog :size="17" />
            </template>
            {{ t("nav.account") }}
          </VBtn>
          <VBtn
            block
            variant="outlined"
            color="primary"
            class="sidebar-logout-button"
            @click="logout"
          >
            <template #prepend>
              <LogOut :size="17" />
            </template>
            {{ t("nav.logout") }}
          </VBtn>
        </div>
      </div>
    </VNavigationDrawer>

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
        @add="openArticleModalFromMobile"
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

    <nav
      v-if="mobileBottomNavigationVisible"
      class="mobile-bottom-nav"
      :aria-label="t('mobile.bottomNavigation')"
    >
      <button
        class="mobile-bottom-nav-item"
        :class="{ 'is-active': isUnreadActive }"
        type="button"
        @click="setStatus('UNREAD')"
      >
        <Circle :size="18" />
        <span>{{ t("nav.unread") }}</span>
      </button>
      <button
        class="mobile-bottom-nav-item"
        :class="{ 'is-active': isAllArticlesActive }"
        type="button"
        @click="setAllArticles"
      >
        <Library :size="18" />
        <span>{{ t("mobile.allArticles") }}</span>
      </button>
      <button
        class="mobile-bottom-nav-item"
        :class="{ 'is-active': isFavoriteActive }"
        type="button"
        @click="setFavoriteOnly"
      >
        <Heart :size="18" />
        <span>{{ t("nav.favorite") }}</span>
      </button>
      <button
        class="mobile-bottom-nav-item"
        :class="{ 'is-active': isCalendarActive }"
        type="button"
        @click="showCalendar"
      >
        <CalendarDays :size="18" />
        <span>{{ t("nav.calendar") }}</span>
      </button>
      <button
        class="mobile-bottom-nav-item mobile-bottom-nav-add"
        type="button"
        @click="openArticleModalFromMobile"
      >
        <Plus :size="20" />
        <span>{{ t("mobile.addArticle") }}</span>
      </button>
    </nav>
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
