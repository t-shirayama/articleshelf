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

const menuOpen = ref(false)

const displayValue = computed({
  get: () => props.modelValue || '',
  set: (value: string) => emit('update:modelValue', value || null)
})

const pickerValue = computed({
  get: () => parseDateValue(props.modelValue),
  set: (value: Date | string | null) => {
    emit('update:modelValue', formatDateValue(value))
    menuOpen.value = false
  }
})

function openDatePicker(): void {
  if (props.disabled) return
  menuOpen.value = true
}

function clearDate(): void {
  emit('update:modelValue', null)
  menuOpen.value = false
}

function parseDateValue(value?: string | null): Date | null {
  if (!value) return null
  const [year, month, day] = value.split('-').map(Number)
  if (!year || !month || !day) return null
  return new Date(year, month - 1, day)
}

function formatDateValue(value: Date | string | null): string | null {
  if (!value) return null
  if (typeof value === 'string') return value.slice(0, 10) || null

  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
</script>

<template>
  <div class="date-field-control">
    <VMenu
      v-model="menuOpen"
      :close-on-content-click="false"
      :disabled="disabled"
      location="bottom start"
      content-class="date-picker-menu"
    >
      <template #activator="{ props: menuProps }">
        <VTextField
          v-bind="{ ...$attrs, ...menuProps }"
          v-model="displayValue"
          class="readstack-date-field"
          :label="label"
          :density="density"
          :disabled="disabled"
          :clearable="clearable"
          readonly
          hide-details
          variant="outlined"
          @click="openDatePicker"
          @click:clear.stop="clearDate"
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
      </template>

      <VDatePicker
        v-model="pickerValue"
        class="readstack-date-picker"
        color="primary"
        show-adjacent-months
      />
    </VMenu>
  </div>
</template>
