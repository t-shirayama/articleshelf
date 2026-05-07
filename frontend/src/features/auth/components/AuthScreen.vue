<script setup lang="ts">
import { computed, ref } from "vue";
import { LogIn, UserPlus } from "lucide-vue-next";
import { useAuthStore } from "../stores/auth";

type AuthMode = "login" | "register";

const authStore = useAuthStore();
const mode = ref<AuthMode>("login");
const email = ref("");
const password = ref("");
const displayName = ref("");
const localError = ref("");

const isRegister = computed(() => mode.value === "register");
const title = computed(() => (isRegister.value ? "ユーザー登録" : "ログイン"));
const submitLabel = computed(() => (isRegister.value ? "登録して始める" : "ログイン"));

async function submit(): Promise<void> {
  localError.value = "";
  try {
    if (isRegister.value) {
      await authStore.register({
        email: email.value,
        password: password.value,
        displayName: displayName.value,
      });
      return;
    }
    await authStore.login({ email: email.value, password: password.value });
  } catch (error: unknown) {
    localError.value =
      error instanceof Error ? error.message : "認証処理に失敗しました";
  }
}

function switchMode(nextMode: AuthMode): void {
  mode.value = nextMode;
  localError.value = "";
  authStore.error = "";
}

function handleModeUpdate(value: unknown): void {
  switchMode(value === "register" ? "register" : "login");
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="auth-title">
      <div class="auth-brand">
        <div class="brand-mark">
          <LogIn v-if="!isRegister" :size="22" />
          <UserPlus v-else :size="22" />
        </div>
        <div>
          <strong>ReadStack</strong>
          <span>学びを蓄える記事棚</span>
        </div>
      </div>

      <div class="auth-heading">
        <h1 id="auth-title">{{ title }}</h1>
        <p>
          {{
            isRegister
              ? "アカウントを作成して、自分の記事だけを管理できます。"
              : "登録済みのメールアドレスで続行します。"
          }}
        </p>
      </div>

      <VBtnToggle
        :model-value="mode"
        mandatory
        class="auth-mode-toggle"
        @update:model-value="handleModeUpdate"
      >
        <VBtn value="login">ログイン</VBtn>
        <VBtn value="register">登録</VBtn>
      </VBtnToggle>

      <form class="auth-form" @submit.prevent="submit">
        <VTextField
          v-model="email"
          label="メールアドレス"
          type="email"
          autocomplete="email"
          required
        />
        <VTextField
          v-if="isRegister"
          v-model="displayName"
          label="表示名"
          autocomplete="name"
        />
        <VTextField
          v-model="password"
          label="パスワード"
          type="password"
          :autocomplete="isRegister ? 'new-password' : 'current-password'"
          required
        />

        <div v-if="localError || authStore.error" class="form-error-banner">
          <span>{{ localError || authStore.error }}</span>
        </div>

        <VBtn
          color="primary"
          type="submit"
          :loading="authStore.loading"
          block
          size="large"
        >
          {{ submitLabel }}
        </VBtn>
      </form>
    </section>
  </main>
</template>
