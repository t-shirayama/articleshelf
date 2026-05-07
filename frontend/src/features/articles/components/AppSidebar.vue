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
</script>

<template>
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
        <span>すべての記事</span>
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
        <span>未読</span>
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
        <span>既読</span>
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
        <span>お気に入り</span>
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
        <span>カレンダー</span>
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
        <span>タグ管理</span>
      </VBtn>
    </nav>

    <MotivationCard :card="currentMotivation" />

    <div class="sidebar-account">
      <span>{{ userName }}</span>
      <VBtn
        variant="text"
        color="primary"
        class="sidebar-logout-button"
        @click="emit('logout')"
      >
        <template #prepend>
          <LogOut :size="17" />
        </template>
        ログアウト
      </VBtn>
    </div>
  </aside>
</template>
