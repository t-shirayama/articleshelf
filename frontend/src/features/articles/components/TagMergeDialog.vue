<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { Tag } from "../types";

defineProps<{
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
</script>

<template>
  <VDialog :model-value="Boolean(source)" max-width="460" @update:model-value="!$event && emit('cancel')">
    <VCard class="tag-management-dialog">
      <VCardTitle>{{ t("tags.mergeTitle") }}</VCardTitle>
      <VCardText class="tag-management-dialog-body">
        <p>
          <strong>{{ source?.name }}</strong>
          {{ t("tags.mergeBody", { name: source?.name || '' }) }}
        </p>
        <VSelect
          :model-value="targetId"
          class="readstack-select"
          :label="t('tags.mergeTarget')"
          :items="options"
          item-title="title"
          item-value="value"
          @update:model-value="emit('update:targetId', String($event))"
        />
      </VCardText>
      <VCardActions>
        <VSpacer />
        <VBtn variant="text" @click="emit('cancel')">{{ t("common.cancel") }}</VBtn>
        <VBtn
          class="action-button action-button-primary"
          color="primary"
          variant="flat"
          :loading="saving"
          :disabled="saving || !targetId"
          @click="emit('confirm')"
        >
          {{ t("tags.mergeConfirm") }}
        </VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
