<script setup lang="ts">
import { onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import { useLocale } from "vuetify";
import AuthScreen from "./features/auth/components/AuthScreen.vue";
import { useAuthStore } from "./features/auth/stores/auth";
import ArticleWorkspace from "./features/articles/views/ArticleWorkspace.vue";

const authStore = useAuthStore();
const { locale } = useI18n({ useScope: "global" });
const { current } = useLocale();
const route = useRoute();
const router = useRouter();

onMounted(() => {
  void authStore.initialize();
});

watch(
  locale,
  (value) => {
    current.value = value;
    document.documentElement.lang = value;
  },
  { immediate: true }
);

watch(
  () => [authStore.authReady, authStore.isAuthenticated, route.path] as const,
  ([authReady, isAuthenticated, path]) => {
    if (!authReady) return;
    const isAuthRoute = path === "/login" || path === "/register";
    if (isAuthenticated && isAuthRoute) {
      void router.replace("/articles");
      return;
    }
    if (!isAuthenticated && !isAuthRoute) {
      void router.replace("/login");
    }
  },
  { immediate: true }
);
</script>

<template>
  <VApp>
    <div v-if="!authStore.authReady" class="auth-loading">
      <VProgressCircular indeterminate color="primary" />
    </div>
    <ArticleWorkspace v-else-if="authStore.isAuthenticated" />
    <AuthScreen v-else />
  </VApp>
</template>
