<script setup lang="ts">
import {
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Circle,
  Heart,
  Library,
  LogOut,
  Tags,
} from "lucide-vue-next";
import { useI18n } from "vue-i18n";
import { getCurrentLocale, setCurrentLocale } from "../../../shared/i18n";
import type { SupportedLocale } from "../../../shared/i18n/locales";
import MotivationCard from "./MotivationCard.vue";
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
}>();

const emit = defineEmits<{
  allArticles: [];
  status: [status: ArticleStatus];
  favoriteOnly: [];
  calendar: [];
  tags: [];
  logout: [];
}>();

const { t, locale } = useI18n({ useScope: "global" });

function changeLocale(value: unknown): void {
  const nextLocale: SupportedLocale = value === "ja" ? "ja" : "en";
  setCurrentLocale(nextLocale);
  locale.value = getCurrentLocale();
}
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark">
        <BookOpen :size="22" />
      </div>
      <div>
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
        @click="emit('allArticles')"
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
        @click="emit('favoriteOnly')"
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
      <div class="side-nav-divider" aria-hidden="true" />
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

    <MotivationCard :card="currentMotivation" />

    <div class="sidebar-account">
      <span>{{ userName }}</span>
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
        class="sidebar-logout-button"
        @click="emit('logout')"
      >
        <template #prepend>
          <LogOut :size="17" />
        </template>
        {{ t("nav.logout") }}
      </VBtn>
    </div>
  </aside>
</template>
