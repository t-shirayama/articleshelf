<script setup lang="ts">
import { computed, ref } from 'vue'
import { CalendarDays } from 'lucide-vue-next'

defineOptions({
  inheritAttrs: false
})

const props = withDefaults(defineProps<{
  modelValue?: string | null
  label?: string
  disabled?: boolean
  clearable?: boolean
  density?: 'default' | 'comfortable' | 'compact'
}>(), {
  modelValue: null,
  label: '',
  disabled: false,
  clearable: false,
  density: 'comfortable'
})

const emit = defineEmits<{
  'update:modelValue': [value: string | null]
}>()

const fieldRoot = ref<HTMLElement | null>(null)

const dateValue = computed({
  get: () => props.modelValue || '',
  set: (value: string) => emit('update:modelValue', value || null)
})

function openDatePicker(): void {
  if (props.disabled) return

  const input = fieldRoot.value?.querySelector<HTMLInputElement>('input[type="date"]')
  if (!input) return

  input.focus()

  const inputWithPicker = input as HTMLInputElement & { showPicker?: () => void }
  if (typeof inputWithPicker.showPicker === 'function') {
    inputWithPicker.showPicker()
    return
  }

  input.click()
}
</script>

<template>
  <div ref="fieldRoot" class="date-field-control">
    <VTextField
      v-bind="$attrs"
      v-model="dateValue"
      class="readstack-date-field"
      type="date"
      :label="label"
      :density="density"
      :disabled="disabled"
      :clearable="clearable"
      hide-details
      variant="outlined"
    >
      <template #append-inner>
        <button
          class="date-picker-button"
          type="button"
          :disabled="disabled"
          :aria-label="`${label || '日付'}のカレンダーを開く`"
          @mousedown.prevent
          @click.stop="openDatePicker"
        >
          <CalendarDays :size="18" />
        </button>
      </template>
    </VTextField>
  </div>
</template>
