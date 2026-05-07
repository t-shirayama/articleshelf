<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { LogIn, UserPlus } from "lucide-vue-next";
import { useAuthStore } from "../stores/auth";

type AuthMode = "login" | "register";

const authStore = useAuthStore();
const { t } = useI18n();
const mode = ref<AuthMode>("login");
const email = ref("");
const password = ref("");
const displayName = ref("");
const localError = ref("");

const isRegister = computed(() => mode.value === "register");
const title = computed(() => (isRegister.value ? t("auth.registerTitle") : t("auth.login")));
const submitLabel = computed(() => (isRegister.value ? t("auth.submitRegister") : t("auth.login")));
const submitted = ref(false);
const emailError = computed(() => {
  const value = email.value.trim();
  if (!value) return t("auth.errors.emailRequired");
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
    ? ""
    : t("auth.errors.emailInvalid");
});
const displayNameError = computed(() => {
  if (!isRegister.value) return "";
  return displayName.value.trim() ? "" : t("auth.errors.displayNameRequired");
});
const passwordError = computed(() => {
  if (!password.value) return t("auth.errors.passwordRequired");
  if (isRegister.value && password.value.length < 8) {
    return t("auth.errors.passwordLength");
  }
  return "";
});
const formValid = computed(
  () => !emailError.value && !displayNameError.value && !passwordError.value,
);

async function submit(): Promise<void> {
  localError.value = "";
  submitted.value = true;
  if (!formValid.value || authStore.loading) return;
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
      error instanceof Error ? error.message : t("auth.errors.authFailed");
  }
}

function switchMode(nextMode: AuthMode): void {
  mode.value = nextMode;
  submitted.value = false;
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
          <strong>{{ t("common.appName") }}</strong>
          <span>{{ t("common.appTagline") }}</span>
        </div>
      </div>

      <div class="auth-heading">
        <h1 id="auth-title">{{ title }}</h1>
        <p>
          {{
            isRegister
              ? t("auth.registerDescription")
              : t("auth.loginDescription")
          }}
        </p>
      </div>

      <VBtnToggle
        :model-value="mode"
        mandatory
        class="auth-mode-toggle"
        @update:model-value="handleModeUpdate"
      >
        <VBtn value="login">{{ t("auth.login") }}</VBtn>
        <VBtn value="register">{{ t("auth.register") }}</VBtn>
      </VBtnToggle>

      <form class="auth-form" @submit.prevent="submit">
        <div class="auth-field">
          <label class="auth-field-label" for="auth-email">{{ t("auth.email") }}</label>
          <VTextField
            id="auth-email"
            v-model="email"
            type="email"
            autocomplete="username"
            placeholder="name@example.com"
            required
            :error-messages="submitted && emailError ? [emailError] : []"
          />
        </div>
        <div v-if="isRegister" class="auth-field">
          <label class="auth-field-label" for="auth-display-name">{{ t("auth.displayName") }}</label>
          <VTextField
            id="auth-display-name"
            v-model="displayName"
            autocomplete="name"
            :placeholder="t('auth.displayNamePlaceholder')"
            :error-messages="submitted && displayNameError ? [displayNameError] : []"
          />
        </div>
        <div class="auth-field">
          <label class="auth-field-label" for="auth-password">{{ t("auth.password") }}</label>
          <VTextField
            id="auth-password"
            v-model="password"
            type="password"
            :autocomplete="isRegister ? 'new-password' : 'current-password'"
            required
            :error-messages="submitted && passwordError ? [passwordError] : []"
          />
        </div>

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
