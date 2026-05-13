<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { Tag } from "../types";

const props = defineProps<{
  source: Tag | null;
  targetId: string;
  options: Array<{ title: string; value: string }>;
  saving: boolean;
}>();

const emit = defineEmits<{
  "update:targetId": [value: string];
  cancel: [];
  confirm: [];
}>();

const { t } = useI18n();

function handleDialogUpdate(open: boolean): void {
  if (!open && !props.saving) emit("cancel");
}
</script>

<template>
  <VDialog :model-value="Boolean(props.source)" max-width="460" content-class="tag-dialog-overlay" @update:model-value="handleDialogUpdate">
    <VCard class="tag-management-dialog" :aria-busy="props.saving">
      <span v-if="props.saving" class="sr-only" role="status" aria-live="polite">
        {{ t("common.merging") }}
      </span>
      <VCardTitle>{{ t("tags.mergeTitle") }}</VCardTitle>
      <VCardText class="tag-management-dialog-body">
        <p>
          <strong>{{ props.source?.name }}</strong>
          {{ t("tags.mergeBody", { name: props.source?.name || '' }) }}
        </p>
        <VSelect
          :model-value="props.targetId"
          class="articleshelf-select"
          :label="t('tags.mergeTarget')"
          :items="props.options"
          item-title="title"
          item-value="value"
          :disabled="props.saving"
          @update:model-value="emit('update:targetId', String($event))"
        />
      </VCardText>
      <VCardActions>
        <VSpacer />
        <VBtn variant="text" :disabled="props.saving" @click="emit('cancel')">{{ t("common.cancel") }}</VBtn>
        <VBtn
          class="action-button action-button-primary"
          color="primary"
          variant="flat"
          :loading="props.saving"
          :disabled="props.saving || !props.targetId"
          @click="emit('confirm')"
        >
          {{ props.saving ? t("common.merging") : t("tags.mergeConfirm") }}
        </VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
