<script setup lang="ts">
import { useI18n } from "vue-i18n";

defineProps<{
  open: boolean;
}>();

const emit = defineEmits<{
  cancel: [];
  confirm: [];
}>();

const { t } = useI18n();
</script>

<template>
  <VDialog
    :model-value="open"
    max-width="420"
    @update:model-value="(value) => { if (!value) emit('cancel') }"
  >
    <VCard class="delete-confirm-dialog" :title="t('dialogs.unsavedTitle')">
      <VCardText>
        <p>
          {{ t("dialogs.unsavedBody") }}
        </p>
      </VCardText>
      <VCardActions>
        <VSpacer />
        <VBtn
          class="action-button action-button-secondary"
          variant="outlined"
          @click="emit('cancel')"
          >{{ t("dialogs.continueEditing") }}</VBtn
        >
        <VBtn
          class="action-button action-button-danger"
          color="error"
          variant="flat"
          @click="emit('confirm')"
          >{{ t("dialogs.discardAndMove") }}</VBtn
        >
      </VCardActions>
    </VCard>
  </VDialog>
</template>
