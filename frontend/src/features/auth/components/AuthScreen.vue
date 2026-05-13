<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import {
  BarChart3,
  BookMarked,
  BookOpen,
  Bookmark,
  LogIn,
  Tags,
  UserPlus,
} from "lucide-vue-next";
import { errorMessage } from "../../../shared/errors";
import { useAuthStore } from "../stores/auth";

type AuthMode = "login" | "register";

const props = withDefaults(
  defineProps<{
    mode?: AuthMode;
  }>(),
  {
    mode: "login",
  },
);

const authStore = useAuthStore();
const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const mode = ref<AuthMode>(props.mode);
const username = ref("");
const password = ref("");
const displayName = ref("");
const localError = ref("");
const authFlowPending = ref(false);

const isRegister = computed(() => mode.value === "register");
const isProcessing = computed(() => authStore.loading || authFlowPending.value);
const title = computed(() => (isRegister.value ? t("auth.registerTitle") : t("auth.login")));
const submitLabel = computed(() => (isRegister.value ? t("auth.submitRegister") : t("auth.login")));
const processingLabel = computed(() => (
  isRegister.value ? t("auth.registerProcessing") : t("auth.loginProcessing")
));
const submitButtonLabel = computed(() => (isProcessing.value ? processingLabel.value : submitLabel.value));
const submitted = ref(false);
const usernameError = computed(() => {
  const value = username.value.trim().toLowerCase();
  if (!value) return t("auth.errors.usernameRequired");
  return /^[a-z0-9._-]{3,32}$/.test(value)
    ? ""
    : t("auth.errors.usernameInvalid");
});
const passwordError = computed(() => {
  if (!password.value) return t("auth.errors.passwordRequired");
  if (isRegister.value && password.value.length < 8) {
    return t("auth.errors.passwordLength");
  }
  return "";
});
const formValid = computed(
  () => !usernameError.value && !passwordError.value,
);

async function submit(): Promise<void> {
  localError.value = "";
  submitted.value = true;
  if (!formValid.value || isProcessing.value) return;
  authFlowPending.value = true;
  try {
    if (isRegister.value) {
      await authStore.register({
        username: username.value.trim().toLowerCase(),
        password: password.value,
        displayName: displayName.value.trim(),
      });
      await router.push(resolvePostAuthPath());
      authFlowPending.value = false;
      return;
    }
    await authStore.login({ username: username.value.trim().toLowerCase(), password: password.value });
    await router.push(resolvePostAuthPath());
    authFlowPending.value = false;
  } catch (error: unknown) {
    localError.value =
      errorMessage(error, t("auth.errors.authFailed"));
    authFlowPending.value = false;
  }
}

function resolvePostAuthPath(): string {
  const returnTo = typeof route.query.returnTo === "string" ? route.query.returnTo : "";
  return returnTo.startsWith("/") ? returnTo : "/articles";
}

function switchMode(nextMode: AuthMode): void {
  if (isProcessing.value) return;
  mode.value = nextMode;
  submitted.value = false;
  localError.value = "";
  authStore.error = "";
}

function handleModeUpdate(value: unknown): void {
  const nextMode = value === "register" ? "register" : "login";
  switchMode(nextMode);

  const nextPath = nextMode === "register" ? "/register" : "/login";
  if (route.path !== nextPath) {
    void router.push({ path: nextPath, query: route.query });
  }
}

watch(
  () => props.mode,
  (value) => {
    switchMode(value);
  },
  { immediate: true },
);
</script>

<template>
  <main class="auth-page">
    <div class="auth-layout">
      <section class="auth-showcase" aria-labelledby="auth-showcase-title">
        <div class="auth-showcase-brand">
          <div class="brand-mark">
            <BookOpen :size="25" />
          </div>
          <strong>{{ t("common.appName") }}</strong>
        </div>

        <div class="auth-showcase-copy">
          <h2 id="auth-showcase-title">{{ t("auth.heroTitle") }}</h2>
          <p>{{ t("auth.heroBody") }}</p>
        </div>

        <div class="auth-benefits" :aria-label="t('auth.benefits.label')">
          <div class="auth-benefit">
            <span class="auth-benefit-icon"><Bookmark :size="22" /></span>
            <div>
              <strong>{{ t("auth.benefits.saveTitle") }}</strong>
              <span>{{ t("auth.benefits.saveBody") }}</span>
            </div>
          </div>
          <div class="auth-benefit">
            <span class="auth-benefit-icon"><Tags :size="22" /></span>
            <div>
              <strong>{{ t("auth.benefits.tagTitle") }}</strong>
              <span>{{ t("auth.benefits.tagBody") }}</span>
            </div>
          </div>
          <div class="auth-benefit">
            <span class="auth-benefit-icon"><BarChart3 :size="22" /></span>
            <div>
              <strong>{{ t("auth.benefits.progressTitle") }}</strong>
              <span>{{ t("auth.benefits.progressBody") }}</span>
            </div>
          </div>
        </div>

        <div class="auth-visual-stack" aria-hidden="true">
          <div class="auth-floating-card auth-progress-card">
            <span>{{ t("auth.visual.progressLabel") }}</span>
            <div class="auth-progress-row">
              <div class="auth-progress-ring">68%</div>
              <div>
                <strong>{{ t("auth.visual.progressTitle") }}</strong>
                <small>{{ t("auth.visual.progressBody") }}</small>
                <i></i>
              </div>
            </div>
          </div>

          <div class="auth-floating-card auth-recent-card">
            <span>{{ t("auth.visual.recentLabel") }}</span>
            <ul>
              <li>
                <b></b>
                <div>
                  <strong>{{ t("auth.visual.articleOne") }}</strong>
                  <small>id-engineering.com</small>
                </div>
                <em>{{ t("auth.visual.timeOne") }}</em>
              </li>
              <li>
                <b></b>
                <div>
                  <strong>{{ t("auth.visual.articleTwo") }}</strong>
                  <small>note.com</small>
                </div>
                <em>{{ t("auth.visual.timeTwo") }}</em>
              </li>
              <li>
                <b></b>
                <div>
                  <strong>{{ t("auth.visual.articleThree") }}</strong>
                  <small>example.com</small>
                </div>
                <em>{{ t("auth.visual.timeThree") }}</em>
              </li>
            </ul>
          </div>

          <div class="auth-floating-card auth-tags-card">
            <span>{{ t("auth.visual.tagsLabel") }}</span>
            <div class="auth-tag-cloud">
              <small>{{ t("auth.visual.tagDesign") }}</small>
              <small>{{ t("auth.visual.tagTech") }}</small>
              <small>{{ t("auth.visual.tagBusiness") }}</small>
              <small>{{ t("auth.visual.tagProduct") }}</small>
            </div>
          </div>

          <BookMarked class="auth-book-illustration" :size="126" />
        </div>
      </section>

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

        <div class="auth-mode-switch">
          <div class="auth-mode-switch-copy">
            <span>{{ t("auth.modeSwitchLabel") }}</span>
          </div>
          <VBtnToggle
            :model-value="mode"
            mandatory
            class="auth-mode-toggle"
            :aria-label="t('auth.modeSwitchLabel')"
            @update:model-value="handleModeUpdate"
          >
            <VBtn value="login" :disabled="isProcessing">
              <LogIn :size="17" />
              <span>{{ t("auth.login") }}</span>
            </VBtn>
            <VBtn value="register" :disabled="isProcessing">
              <UserPlus :size="17" />
              <span>{{ t("auth.register") }}</span>
            </VBtn>
          </VBtnToggle>
        </div>

        <form class="auth-form" :aria-busy="isProcessing" @submit.prevent="submit">
          <div class="auth-field">
            <label class="auth-field-label" for="auth-username">{{ t("auth.username") }}</label>
            <VTextField
              id="auth-username"
              v-model="username"
              type="text"
              autocomplete="username"
              :placeholder="t('auth.usernamePlaceholder')"
              :hint="isRegister ? t('auth.usernameHelp') : undefined"
              :persistent-hint="isRegister"
              required
              :disabled="isProcessing"
              :error-messages="submitted && usernameError ? [usernameError] : []"
            />
          </div>
          <div v-if="isRegister" class="auth-field">
            <label class="auth-field-label" for="auth-display-name">{{ t("auth.displayName") }}</label>
            <VTextField
              id="auth-display-name"
              v-model="displayName"
              autocomplete="name"
              :placeholder="t('auth.displayNamePlaceholder')"
              :hint="t('auth.displayNameHelp')"
              persistent-hint
              :disabled="isProcessing"
            />
          </div>
          <div class="auth-field">
            <label class="auth-field-label" for="auth-password">{{ t("auth.password") }}</label>
            <VTextField
              id="auth-password"
              v-model="password"
              type="password"
              :autocomplete="isRegister ? 'new-password' : 'current-password'"
              :hint="isRegister ? t('auth.passwordHelp') : undefined"
              :persistent-hint="isRegister"
              required
              :disabled="isProcessing"
              :error-messages="submitted && passwordError ? [passwordError] : []"
            />
          </div>

          <div v-if="localError || authStore.error" class="form-error-banner">
            <span>{{ localError || authStore.error }}</span>
          </div>

          <span v-if="isProcessing" class="sr-only" role="status" aria-live="polite">
            {{ processingLabel }}
          </span>

          <VBtn
            color="primary"
            type="submit"
            :loading="isProcessing"
            :disabled="isProcessing"
            block
            size="large"
          >
            {{ submitButtonLabel }}
          </VBtn>
        </form>
      </section>
    </div>
  </main>
</template>
