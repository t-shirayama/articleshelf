<script setup lang="ts">
import type { Article } from "../types";

defineProps<{
  article: Article | null;
}>();

const emit = defineEmits<{
  cancel: [];
  confirm: [];
}>();
</script>

<template>
  <VDialog
    :model-value="Boolean(article)"
    max-width="420"
    @update:model-value="(value) => { if (!value) emit('cancel') }"
  >
    <VCard class="delete-confirm-dialog" title="記事を削除しますか？">
      <VCardText>
        <p>
          「{{ article?.title }}」を削除します。この操作は取り消せません。
        </p>
      </VCardText>
      <VCardActions>
        <VSpacer />
        <VBtn
          class="action-button action-button-secondary"
          variant="outlined"
          @click="emit('cancel')"
          >キャンセル</VBtn
        >
        <VBtn
          class="action-button action-button-danger"
          color="error"
          variant="flat"
          @click="emit('confirm')"
          >削除する</VBtn
        >
      </VCardActions>
    </VCard>
  </VDialog>
</template>
