<script setup lang="ts">
import { useI18n } from "vue-i18n";

const props = defineProps<{
  open: boolean;
  message: string;
  primaryActionLabel?: string;
  secondaryActionLabel?: string;
}>();

const emit = defineEmits<{
  "update:open": [value: boolean];
  undo: [];
  "primary-action": [];
  "secondary-action": [];
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
      <VBtn
        v-if="props.primaryActionLabel"
        class="snackbar-primary-action"
        variant="text"
        @click="emit('primary-action')"
      >
        {{ props.primaryActionLabel }}
      </VBtn>
      <VBtn
        v-if="props.secondaryActionLabel"
        class="snackbar-secondary-action"
        variant="text"
        @click="emit('secondary-action')"
      >
        {{ props.secondaryActionLabel }}
      </VBtn>
      <VBtn v-if="!props.primaryActionLabel && !props.secondaryActionLabel" class="undo-button" variant="text" @click="emit('undo')">{{ t("articles.undo") }}</VBtn>
    </template>
  </VSnackbar>
</template>
