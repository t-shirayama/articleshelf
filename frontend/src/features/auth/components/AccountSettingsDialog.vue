<script setup lang="ts">
import { ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { KeyRound, LogOut, Trash2, X } from "lucide-vue-next";

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
const currentPassword = ref("");
const newPassword = ref("");
const deletePassword = ref("");

watch(
  () => props.open,
  (open) => {
    if (open) return;
    currentPassword.value = "";
    newPassword.value = "";
    deletePassword.value = "";
  },
);

function submitPasswordChange(): void {
  emit("changePassword", {
    currentPassword: currentPassword.value,
    newPassword: newPassword.value,
  });
}

function submitDeleteAccount(): void {
  emit("deleteAccount", { currentPassword: deletePassword.value });
}
</script>

<template>
  <VDialog
    :model-value="open"
    max-width="560"
    content-class="account-dialog-overlay"
    @update:model-value="!$event && emit('close')"
  >
    <VCard class="account-settings-dialog">
      <header class="article-modal-header account-settings-header">
        <h2>{{ t("auth.account.title") }}</h2>
        <div class="article-modal-header-actions">
          <VBtn
            icon
            variant="text"
            :aria-label="t('common.close')"
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
              :loading="loading"
              :disabled="!currentPassword || !newPassword"
              @click="submitPasswordChange"
            >
              {{ t("auth.account.changePassword") }}
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
            variant="outlined"
            color="primary"
            :loading="loading"
            @click="emit('logoutAll')"
          >
            {{ t("auth.account.logoutAll") }}
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
            :label="t('auth.account.currentPassword')"
            :disabled="loading"
          />
          <VBtn
            color="error"
            :loading="loading"
            :disabled="!deletePassword"
            @click="submitDeleteAccount"
          >
            {{ t("auth.account.deleteAccount") }}
          </VBtn>
        </section>
      </VCardText>
    </VCard>
  </VDialog>
</template>
