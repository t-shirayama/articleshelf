<script setup lang="ts">
const props = defineProps<{
  open: boolean;
  message: string;
}>();

const emit = defineEmits<{
  "update:open": [value: boolean];
  undo: [];
}>();
</script>

<template>
  <VSnackbar
    :model-value="props.open"
    timeout="2000"
    @update:model-value="emit('update:open', $event)"
  >
    {{ message }}
    <template #actions>
      <VBtn variant="text" @click="emit('undo')">元に戻す</VBtn>
    </template>
  </VSnackbar>
</template>
