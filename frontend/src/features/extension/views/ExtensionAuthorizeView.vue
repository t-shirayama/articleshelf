<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import { extensionAuthApi } from "../api/extensionAuthApi";

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const error = ref("");

onMounted(() => {
  void authorizeExtension();
});

async function authorizeExtension(): Promise<void> {
  const input = readAuthorizeInput();
  if (!input) {
    error.value = t("extensionAuth.invalidRequest");
    return;
  }

  try {
    const response = await extensionAuthApi.authorize(input);
    const redirectUrl = new URL(response.redirectUri);
    redirectUrl.searchParams.set("code", response.code);
    redirectUrl.searchParams.set("state", response.state);
    window.location.assign(redirectUrl.toString());
  } catch {
    error.value = t("extensionAuth.failed");
  }
}

function readAuthorizeInput() {
  const clientId = readQuery("client_id");
  const extensionId = readQuery("extension_id");
  const redirectUri = readQuery("redirect_uri");
  const state = readQuery("state");
  const codeChallenge = readQuery("code_challenge");
  const codeChallengeMethod = readQuery("code_challenge_method");
  if (!clientId || !extensionId || !redirectUri || !state || !codeChallenge || !codeChallengeMethod) {
    return null;
  }
  return {
    clientId,
    extensionId,
    redirectUri,
    state,
    codeChallenge,
    codeChallengeMethod
  };
}

function readQuery(name: string): string {
  const value = route.query[name];
  return typeof value === "string" ? value : "";
}

function goHome(): void {
  void router.push("/articles");
}
</script>

<template>
  <main class="extension-auth-page">
    <section class="extension-auth-panel">
      <p class="eyebrow">{{ t("common.appName") }}</p>
      <h1>{{ t("extensionAuth.title") }}</h1>
      <p v-if="!error">{{ t("extensionAuth.processing") }}</p>
      <div v-else class="form-error-banner" role="alert">
        <span>{{ error }}</span>
      </div>
      <VProgressCircular v-if="!error" indeterminate color="primary" />
      <VBtn v-else color="primary" @click="goHome">
        {{ t("extensionAuth.backToArticles") }}
      </VBtn>
    </section>
  </main>
</template>
