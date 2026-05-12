<script setup lang="ts">
import {
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Circle,
  Heart,
  Library,
  Download,
  LogOut,
  Menu,
  Plus,
  Tags,
  UserCog,
} from "lucide-vue-next";
import { useI18n } from "vue-i18n";
import type { SupportedLocale } from "../../../shared/i18n/locales";
import AppSidebar from "./AppSidebar.vue";
import type { ArticleStatus, MotivationCardData } from "../types";

defineProps<{
  counts: {
    all: number;
    unread: number;
    read: number;
    favorite: number;
  };
  currentMotivation: MotivationCardData;
  isAllArticlesActive: boolean;
  isUnreadActive: boolean;
  isReadActive: boolean;
  isFavoriteActive: boolean;
  isCalendarActive: boolean;
  isTagsActive: boolean;
  userName: string;
  drawerOpen: boolean;
  bottomNavigationVisible: boolean;
  currentLocale: SupportedLocale;
  detailShell: boolean;
}>();

const emit = defineEmits<{
  "update:drawerOpen": [value: boolean];
  "all-articles": [];
  "status": [status: ArticleStatus];
  "favorite-only": [];
  "calendar": [];
  "tags": [];
  "account": [];
  "logout": [];
  "change-locale": [value: SupportedLocale];
  "add-article": [];
}>();

const { t } = useI18n({ useScope: "global" });
const extensionDownloadUrl =
  import.meta.env.VITE_EXTENSION_DOWNLOAD_URL ??
  "https://github.com/t-shirayama/articleshelf/releases/latest/download/articleshelf-chrome-extension.zip";

function setDrawerOpen(value: boolean): void {
  emit("update:drawerOpen", value);
}
</script>

<template>
  <div class="app-shell" :class="{ 'is-detail-shell': detailShell }">
    <header class="mobile-app-header">
      <VBtn
        class="mobile-menu-button"
        variant="text"
        :aria-label="t('mobile.openMenu')"
        @click="setDrawerOpen(true)"
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
      :counts="counts"
      :current-motivation="currentMotivation"
      :is-all-articles-active="isAllArticlesActive"
      :is-unread-active="isUnreadActive"
      :is-read-active="isReadActive"
      :is-favorite-active="isFavoriteActive"
      :is-calendar-active="isCalendarActive"
      :is-tags-active="isTagsActive"
      :user-name="userName"
      @all-articles="emit('all-articles')"
      @status="emit('status', $event)"
      @favorite-only="emit('favorite-only')"
      @calendar="emit('calendar')"
      @tags="emit('tags')"
      @account="emit('account')"
      @logout="emit('logout')"
    />

    <VNavigationDrawer
      :model-value="drawerOpen"
      class="mobile-navigation-drawer"
      temporary
      width="304"
      :aria-label="t('mobile.menu')"
      @update:model-value="setDrawerOpen"
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
            @click="emit('all-articles')"
          >
            <template #prepend>
              <Library :size="18" />
            </template>
            <span>{{ t("nav.allArticles") }}</span>
            <strong>{{ counts.all }}</strong>
          </VBtn>
          <VBtn
            class="side-nav-item"
            :class="{ 'is-active': isUnreadActive }"
            block
            variant="text"
            :color="isUnreadActive ? 'primary' : undefined"
            @click="emit('status', 'UNREAD')"
          >
            <template #prepend>
              <Circle :size="18" />
            </template>
            <span>{{ t("nav.unread") }}</span>
            <strong>{{ counts.unread }}</strong>
          </VBtn>
          <VBtn
            class="side-nav-item"
            :class="{ 'is-active': isReadActive }"
            block
            variant="text"
            :color="isReadActive ? 'primary' : undefined"
            @click="emit('status', 'READ')"
          >
            <template #prepend>
              <CheckCircle2 :size="18" />
            </template>
            <span>{{ t("nav.read") }}</span>
            <strong>{{ counts.read }}</strong>
          </VBtn>
          <div class="side-nav-divider" aria-hidden="true" />
          <VBtn
            class="side-nav-item"
            :class="{ 'is-active': isFavoriteActive }"
            block
            variant="text"
            :color="isFavoriteActive ? 'primary' : undefined"
            @click="emit('favorite-only')"
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
            @click="emit('calendar')"
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
            @click="emit('tags')"
          >
            <template #prepend>
              <Tags :size="18" />
            </template>
            <span>{{ t("nav.tagManagement") }}</span>
          </VBtn>
        </nav>

        <div class="sidebar-account mobile-drawer-account">
          <span>{{ userName }}</span>
          <VBtnToggle
            class="sidebar-language-toggle"
            :model-value="currentLocale"
            mandatory
            divided
            density="comfortable"
            color="primary"
            :aria-label="t('locale.label')"
            @update:model-value="emit('change-locale', $event === 'ja' ? 'ja' : 'en')"
          >
            <VBtn value="ja">{{ t("locale.ja") }}</VBtn>
            <VBtn value="en">{{ t("locale.en") }}</VBtn>
          </VBtnToggle>
          <VBtn
            block
            variant="outlined"
            color="primary"
            class="sidebar-account-button"
            :href="extensionDownloadUrl"
            download
          >
            <template #prepend>
              <Download :size="17" />
            </template>
            {{ t("nav.chromeExtension") }}
          </VBtn>
          <VBtn
            block
            variant="outlined"
            color="primary"
            class="sidebar-account-button"
            @click="emit('account')"
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
            @click="emit('logout')"
          >
            <template #prepend>
              <LogOut :size="17" />
            </template>
            {{ t("nav.logout") }}
          </VBtn>
        </div>
      </div>
    </VNavigationDrawer>

    <main class="content" :class="{ 'is-detail-shell': detailShell }">
      <slot />
    </main>

    <nav
      v-if="bottomNavigationVisible"
      class="mobile-bottom-nav"
      :aria-label="t('mobile.bottomNavigation')"
    >
      <button
        class="mobile-bottom-nav-item"
        :class="{ 'is-active': isUnreadActive }"
        type="button"
        @click="emit('status', 'UNREAD')"
      >
        <Circle :size="18" />
        <span>{{ t("nav.unread") }}</span>
      </button>
      <button
        class="mobile-bottom-nav-item"
        :class="{ 'is-active': isAllArticlesActive }"
        type="button"
        @click="emit('all-articles')"
      >
        <Library :size="18" />
        <span>{{ t("mobile.allArticles") }}</span>
      </button>
      <button
        class="mobile-bottom-nav-item"
        :class="{ 'is-active': isFavoriteActive }"
        type="button"
        @click="emit('favorite-only')"
      >
        <Heart :size="18" />
        <span>{{ t("nav.favorite") }}</span>
      </button>
      <button
        class="mobile-bottom-nav-item"
        :class="{ 'is-active': isCalendarActive }"
        type="button"
        @click="emit('calendar')"
      >
        <CalendarDays :size="18" />
        <span>{{ t("nav.calendar") }}</span>
      </button>
      <button
        class="mobile-bottom-nav-item mobile-bottom-nav-add"
        type="button"
        @click="emit('add-article')"
      >
        <Plus :size="20" />
        <span>{{ t("mobile.addArticle") }}</span>
      </button>
    </nav>
  </div>
</template>
