<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { Download, KeyRound, LogOut, Puzzle, Trash2, X } from "lucide-vue-next";

const props = defineProps<{
  open: boolean;
  loading: boolean;
  error: string;
}>();

const emit = defineEmits<{
  close: [];
  changePassword: [input: { currentPassword: string; newPassword: string }];
  logoutAll: [];
  deleteAccount: [input: { currentPassword: string }];
}>();

const { t } = useI18n();

const extensionVersion = ref(
  normalizeVersionLabel(import.meta.env.VITE_EXTENSION_VERSION),
);
const extensionDownloadUrl =
  import.meta.env.VITE_EXTENSION_DOWNLOAD_URL ??
  "https://github.com/t-shirayama/articleshelf/releases/latest/download/articleshelf-chrome-extension.zip";
const currentPassword = ref("");
const newPassword = ref("");
const deletePassword = ref("");
const pendingAction = ref<"password" | "logoutAll" | "delete" | null>(null);
const releaseApiUrl = "https://api.github.com/repos/t-shirayama/articleshelf/releases/latest";

function normalizeVersionLabel(input: string | undefined): string {
  const value = input?.trim() ?? "";
  if (!value) return "latest";
  return value.startsWith("v") ? value.slice(1) : value;
}

async function loadExtensionVersionFromGitHub(): Promise<void> {
  if (!extensionDownloadUrl.includes("github.com/t-shirayama/articleshelf/releases/latest/download/articleshelf-chrome-extension.zip")) return;
  try {
    const response = await fetch(releaseApiUrl, {
      headers: {
        Accept: "application/vnd.github+json",
      },
    });

    if (!response.ok) return;

    const payload = (await response.json()) as { tag_name?: string };
    if (payload.tag_name) {
      extensionVersion.value = normalizeVersionLabel(payload.tag_name);
    }
  } catch {
    // keep fallback value
  }
}

onMounted(() => {
  if (!import.meta.env.VITE_EXTENSION_VERSION) {
    void loadExtensionVersionFromGitHub();
  }
});

watch(
  () => props.open,
  (open) => {
    if (open) return;
    currentPassword.value = "";
    newPassword.value = "";
    deletePassword.value = "";
    pendingAction.value = null;
  },
);

watch(
  () => props.loading,
  (loading) => {
    if (!loading) pendingAction.value = null;
  },
);

function submitPasswordChange(): void {
  if (props.loading) return;
  pendingAction.value = "password";
  emit("changePassword", {
    currentPassword: currentPassword.value,
    newPassword: newPassword.value,
  });
}

function submitLogoutAll(): void {
  if (props.loading) return;
  pendingAction.value = "logoutAll";
  emit("logoutAll");
}

function submitDeleteAccount(): void {
  if (props.loading) return;
  pendingAction.value = "delete";
  emit("deleteAccount", { currentPassword: deletePassword.value });
}

function handleDialogUpdate(open: boolean): void {
  if (!open && !props.loading) emit("close");
}
</script>

<template>
  <VDialog
    :model-value="open"
    max-width="560"
    content-class="account-dialog-overlay"
    @update:model-value="handleDialogUpdate"
  >
    <VCard class="account-settings-dialog" :aria-busy="loading">
      <span v-if="loading" class="sr-only" role="status" aria-live="polite">
        {{
          pendingAction === "password"
            ? t("auth.account.changingPassword")
            : pendingAction === "logoutAll"
              ? t("auth.account.loggingOutAll")
              : pendingAction === "delete"
                ? t("auth.account.deletingAccount")
                : t("common.processing")
        }}
      </span>
      <header class="article-modal-header account-settings-header">
        <h2>{{ t("auth.account.title") }}</h2>
        <div class="article-modal-header-actions">
          <VBtn
            icon
            variant="text"
            :aria-label="t('common.close')"
            :disabled="loading"
            @click="emit('close')"
          >
            <X :size="18" />
          </VBtn>
        </div>
      </header>

      <VCardText class="account-settings-body">
        <div v-if="error" class="form-error-banner" role="alert" aria-live="assertive">
          <span>{{ error }}</span>
        </div>

        <section class="account-settings-section">
          <div class="account-settings-section-heading">
            <Puzzle :size="18" />
            <strong>{{ t("auth.account.extensionTitle") }}</strong>
            <span class="account-settings-chip">
              {{ t("auth.account.extensionVersion", { version: extensionVersion }) }}
            </span>
          </div>
          <p>{{ t("auth.account.extensionDescription") }}</p>
          <div class="account-settings-extension-actions">
            <VBtn
              class="account-change-password-button"
              color="primary"
              variant="flat"
              :href="extensionDownloadUrl"
              download
            >
              <template #prepend>
                <Download :size="18" />
              </template>
              {{ t("auth.account.extensionDownload") }}
            </VBtn>
          </div>
          <ol class="account-settings-steps">
            <li>{{ t("auth.account.extensionStepOne") }}</li>
            <li>{{ t("auth.account.extensionStepTwo") }}</li>
            <li>{{ t("auth.account.extensionStepThree") }}</li>
            <li>{{ t("auth.account.extensionStepFour") }}</li>
          </ol>
          <p>{{ t("auth.account.extensionReinstall") }}</p>
        </section>

        <VDivider />

        <section class="account-settings-section">
          <div class="account-settings-section-heading">
            <KeyRound :size="18" />
            <strong>{{ t("auth.account.changePasswordTitle") }}</strong>
          </div>
          <div class="account-settings-grid">
            <VTextField
              v-model="currentPassword"
              type="password"
              autocomplete="current-password"
              :label="t('auth.account.currentPassword')"
              :disabled="loading"
            />
            <VTextField
              v-model="newPassword"
              type="password"
              autocomplete="new-password"
              :label="t('auth.account.newPassword')"
              :disabled="loading"
            />
            <VBtn
              color="primary"
              :loading="loading && pendingAction === 'password'"
              :disabled="loading || !currentPassword || !newPassword"
              @click="submitPasswordChange"
            >
              {{ loading && pendingAction === "password" ? t("auth.account.changingPassword") : t("auth.account.changePassword") }}
            </VBtn>
          </div>
        </section>

        <VDivider />

        <section class="account-settings-section">
          <div class="account-settings-section-heading">
            <LogOut :size="18" />
            <strong>{{ t("auth.account.logoutAllTitle") }}</strong>
          </div>
          <p>{{ t("auth.account.logoutAllDescription") }}</p>
          <VBtn
            class="account-logout-all-button"
            variant="outlined"
            color="primary"
            :loading="loading && pendingAction === 'logoutAll'"
            :disabled="loading"
            @click="submitLogoutAll"
          >
            {{ loading && pendingAction === "logoutAll" ? t("auth.account.loggingOutAll") : t("auth.account.logoutAll") }}
          </VBtn>
        </section>

        <VDivider />

        <section class="account-settings-section account-settings-danger">
          <div class="account-settings-section-heading">
            <Trash2 :size="18" />
            <strong>{{ t("auth.account.deleteTitle") }}</strong>
          </div>
          <p>{{ t("auth.account.deleteDescription") }}</p>
          <VTextField
            v-model="deletePassword"
            type="password"
            autocomplete="current-password"
            data-delete-password="true"
            :label="t('auth.account.currentPassword')"
            :disabled="loading"
          />
          <VBtn
            class="account-delete-button"
            color="error"
            :loading="loading && pendingAction === 'delete'"
            :disabled="loading || !deletePassword"
            @click="submitDeleteAccount"
          >
            {{ loading && pendingAction === "delete" ? t("auth.account.deletingAccount") : t("auth.account.deleteAccount") }}
          </VBtn>
        </section>
      </VCardText>
    </VCard>
  </VDialog>
</template>
