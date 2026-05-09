<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { Tag } from "../types";

defineProps<{
  candidate: Tag | null;
  saving: boolean;
}>();

const emit = defineEmits<{
  cancel: [];
  confirm: [];
}>();

const { t } = useI18n();
</script>

<template>
  <VDialog :model-value="Boolean(candidate)" max-width="420" content-class="tag-dialog-overlay" @update:model-value="!$event && emit('cancel')">
    <VCard class="tag-management-dialog">
      <VCardTitle>{{ t("tags.deleteUnusedTitle") }}</VCardTitle>
      <VCardText>
        {{ t("tags.deleteUnusedBody", { name: candidate?.name || '' }) }}
      </VCardText>
      <VCardActions>
        <VSpacer />
        <VBtn variant="text" @click="emit('cancel')">{{ t("common.cancel") }}</VBtn>
        <VBtn
          class="action-button action-button-danger"
          color="error"
          variant="flat"
          :loading="saving"
          :disabled="saving"
          @click="emit('confirm')"
        >
          {{ t("common.deleteAction") }}
        </VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
