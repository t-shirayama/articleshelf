<script setup lang="ts">
import { onMounted } from "vue";
import AuthScreen from "./features/auth/components/AuthScreen.vue";
import { useAuthStore } from "./features/auth/stores/auth";
import ArticleWorkspace from "./features/articles/views/ArticleWorkspace.vue";

const authStore = useAuthStore();

onMounted(() => {
  void authStore.initialize();
});
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
