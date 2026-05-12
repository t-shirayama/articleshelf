<script setup lang="ts">
import { watch } from "vue";
import { useI18n } from "vue-i18n";
import { useLocale } from "vuetify";
import { useAuthStore } from "./features/auth/stores/auth";
import { ensureAuthReady } from "./features/auth/services/ensureAuthReady";

const authStore = useAuthStore();
const { locale } = useI18n({ useScope: "global" });
const { current } = useLocale();

void ensureAuthReady();

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
    <RouterView v-else />
  </VApp>
</template>
