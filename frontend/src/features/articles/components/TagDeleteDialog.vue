<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { Tag } from "../types";

const props = defineProps<{
  candidate: Tag | null;
  saving: boolean;
}>();

const emit = defineEmits<{
  cancel: [];
  confirm: [];
}>();

const { t } = useI18n();

function handleDialogUpdate(open: boolean): void {
  if (!open && !props.saving) emit("cancel");
}
</script>

<template>
  <VDialog :model-value="Boolean(props.candidate)" max-width="420" content-class="tag-dialog-overlay" @update:model-value="handleDialogUpdate">
    <VCard class="tag-management-dialog" :aria-busy="props.saving">
      <span v-if="props.saving" class="sr-only" role="status" aria-live="polite">
        {{ t("common.deleting") }}
      </span>
      <VCardTitle>{{ t("tags.deleteUnusedTitle") }}</VCardTitle>
      <VCardText>
        {{ t("tags.deleteUnusedBody", { name: props.candidate?.name || '' }) }}
      </VCardText>
      <VCardActions>
        <VSpacer />
        <VBtn variant="text" :disabled="props.saving" @click="emit('cancel')">{{ t("common.cancel") }}</VBtn>
        <VBtn
          class="action-button action-button-danger"
          color="error"
          variant="flat"
          :loading="props.saving"
          :disabled="props.saving"
          @click="emit('confirm')"
        >
          {{ props.saving ? t("common.deleting") : t("common.deleteAction") }}
        </VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
