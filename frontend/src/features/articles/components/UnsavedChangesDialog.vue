<script setup lang="ts">
defineProps<{
  open: boolean;
}>();

const emit = defineEmits<{
  cancel: [];
  confirm: [];
}>();
</script>

<template>
  <VDialog
    :model-value="open"
    max-width="420"
    @update:model-value="(value) => { if (!value) emit('cancel') }"
  >
    <VCard class="delete-confirm-dialog" title="未保存の編集があります">
      <VCardText>
        <p>
          保存していない変更があります。このまま移動すると編集内容は破棄されます。
        </p>
      </VCardText>
      <VCardActions>
        <VSpacer />
        <VBtn
          class="action-button action-button-secondary"
          variant="outlined"
          @click="emit('cancel')"
          >編集を続ける</VBtn
        >
        <VBtn
          class="action-button action-button-danger"
          color="error"
          variant="flat"
          @click="emit('confirm')"
          >破棄して移動</VBtn
        >
      </VCardActions>
    </VCard>
  </VDialog>
</template>
