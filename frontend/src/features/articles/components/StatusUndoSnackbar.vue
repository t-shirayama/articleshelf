<script setup lang="ts">
import { useI18n } from "vue-i18n";

const props = defineProps<{
  open: boolean;
  message: string;
}>();

const emit = defineEmits<{
  "update:open": [value: boolean];
  undo: [];
}>();

const { t } = useI18n();
</script>

<template>
  <VSnackbar
    :model-value="props.open"
    timeout="2000"
    @update:model-value="emit('update:open', $event)"
  >
    {{ message }}
    <template #actions>
      <VBtn variant="text" @click="emit('undo')">{{ t("articles.undo") }}</VBtn>
    </template>
  </VSnackbar>
</template>
