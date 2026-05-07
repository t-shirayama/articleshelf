<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { Article } from "../types";

defineProps<{
  article: Article | null;
}>();

const emit = defineEmits<{
  cancel: [];
  confirm: [];
}>();

const { t } = useI18n();
</script>

<template>
  <VDialog
    :model-value="Boolean(article)"
    max-width="420"
    @update:model-value="(value) => { if (!value) emit('cancel') }"
  >
    <VCard class="delete-confirm-dialog" :title="t('dialogs.deleteArticleTitle')">
      <VCardText>
        <p>
          {{ t("dialogs.deleteArticleBody", { title: article?.title || "" }) }}
        </p>
      </VCardText>
      <VCardActions>
        <VSpacer />
        <VBtn
          class="action-button action-button-secondary"
          variant="outlined"
          @click="emit('cancel')"
          >{{ t("common.cancel") }}</VBtn
        >
        <VBtn
          class="action-button action-button-danger"
          color="error"
          variant="flat"
          @click="emit('confirm')"
          >{{ t("common.deleteAction") }}</VBtn
        >
      </VCardActions>
    </VCard>
  </VDialog>
</template>
