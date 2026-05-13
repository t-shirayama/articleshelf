<script setup lang="ts">
import { useI18n } from "vue-i18n";

const props = defineProps<{
  open: boolean;
  draft: string;
  saving: boolean;
}>();

const emit = defineEmits<{
  "update:draft": [value: string];
  cancel: [];
  confirm: [];
}>();

const { t } = useI18n();

function handleDialogUpdate(open: boolean): void {
  if (!open && !props.saving) emit("cancel");
}
</script>

<template>
  <VDialog :model-value="props.open" max-width="420" content-class="tag-dialog-overlay" @update:model-value="handleDialogUpdate">
    <VCard class="tag-management-dialog" :aria-busy="props.saving">
      <span v-if="props.saving" class="sr-only" role="status" aria-live="polite">
        {{ t("common.adding") }}
      </span>
      <VCardTitle>{{ t("tags.addTitle") }}</VCardTitle>
      <VCardText class="tag-management-dialog-body">
        <VTextField
          :model-value="props.draft"
          :label="t('tags.name')"
          autofocus
          :disabled="props.saving"
          @update:model-value="emit('update:draft', String($event))"
          @keyup.enter="emit('confirm')"
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
          :disabled="props.saving || !props.draft.trim()"
          @click="emit('confirm')"
        >
          {{ props.saving ? t("common.adding") : t("common.add") }}
        </VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
