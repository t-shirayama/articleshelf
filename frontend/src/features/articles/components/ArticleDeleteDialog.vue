<script setup lang="ts">
import { useI18n } from "vue-i18n";

const props = defineProps<{
  open: boolean;
  title: string;
  deleting: boolean;
}>();

const emit = defineEmits<{
  "update:open": [value: boolean];
  confirm: [];
}>();

const { t } = useI18n();

function handleDialogUpdate(open: boolean): void {
  if (!open && !props.deleting) emit("update:open", false);
}
</script>

<template>
  <VDialog :model-value="props.open" max-width="420" @update:model-value="handleDialogUpdate">
    <VCard class="delete-confirm-dialog" :title="t('dialogs.deleteArticleTitle')">
      <VCardText>
        <p>
          {{ t('dialogs.deleteArticleBody', { title: props.title }) }}
        </p>
      </VCardText>
      <VCardActions :aria-busy="props.deleting">
        <span v-if="props.deleting" class="sr-only" role="status" aria-live="polite">
          {{ t('common.deleting') }}
        </span>
        <VSpacer />
        <VBtn class="action-button action-button-secondary" variant="outlined" :disabled="props.deleting" @click="emit('update:open', false)">{{ t('common.cancel') }}</VBtn>
        <VBtn class="action-button action-button-danger" color="error" variant="flat" :loading="props.deleting" :disabled="props.deleting" @click="emit('confirm')">{{ props.deleting ? t('common.deleting') : t('common.deleteAction') }}</VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
