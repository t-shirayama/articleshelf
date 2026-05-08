<script setup lang="ts">
import { useI18n } from "vue-i18n";

defineProps<{
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
</script>

<template>
  <VDialog :model-value="open" max-width="420" @update:model-value="!$event && emit('cancel')">
    <VCard class="tag-management-dialog">
      <VCardTitle>{{ t("tags.addTitle") }}</VCardTitle>
      <VCardText class="tag-management-dialog-body">
        <VTextField
          :model-value="draft"
          :label="t('tags.name')"
          autofocus
          :disabled="saving"
          @update:model-value="emit('update:draft', String($event))"
          @keyup.enter="emit('confirm')"
        />
      </VCardText>
      <VCardActions>
        <VSpacer />
        <VBtn variant="text" :disabled="saving" @click="emit('cancel')">{{ t("common.cancel") }}</VBtn>
        <VBtn
          class="action-button action-button-primary"
          color="primary"
          variant="flat"
          :loading="saving"
          :disabled="saving || !draft.trim()"
          @click="emit('confirm')"
        >
          {{ t("common.add") }}
        </VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
