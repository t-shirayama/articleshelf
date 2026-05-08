<script setup lang="ts">
import { useI18n } from "vue-i18n";

defineProps<{
  open: boolean;
  title: string;
  deleting: boolean;
}>();

const emit = defineEmits<{
  "update:open": [value: boolean];
  confirm: [];
}>();

const { t } = useI18n();
</script>

<template>
  <VDialog :model-value="open" max-width="420" @update:model-value="emit('update:open', Boolean($event))">
    <VCard class="delete-confirm-dialog" :title="t('dialogs.deleteArticleTitle')">
      <VCardText>
        <p>
          {{ t('dialogs.deleteArticleBody', { title }) }}
        </p>
      </VCardText>
      <VCardActions>
        <VSpacer />
        <VBtn class="action-button action-button-secondary" variant="outlined" @click="emit('update:open', false)">{{ t('common.cancel') }}</VBtn>
        <VBtn class="action-button action-button-danger" color="error" variant="flat" :loading="deleting" :disabled="deleting" @click="emit('confirm')">{{ t('common.deleteAction') }}</VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
