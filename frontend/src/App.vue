<script setup lang="ts">
import { onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useLocale } from "vuetify";
import AuthScreen from "./features/auth/components/AuthScreen.vue";
import { useAuthStore } from "./features/auth/stores/auth";
import ArticleWorkspace from "./features/articles/views/ArticleWorkspace.vue";

const authStore = useAuthStore();
const { locale } = useI18n({ useScope: "global" });
const { current } = useLocale();

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
