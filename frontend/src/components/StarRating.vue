<script setup lang="ts">
import { Star } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  modelValue?: number
  readonly?: boolean
  size?: number
}>(), {
  modelValue: 0,
  readonly: false,
  size: 24
})

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

function selectRating(value: number): void {
  if (props.readonly) return
  emit('update:modelValue', value)
}
</script>

<template>
  <div class="star-rating" :class="{ 'is-readonly': readonly }" :aria-label="`おすすめ度 ${modelValue} / 5`">
    <button
      v-for="star in 5"
      :key="star"
      type="button"
      class="star-rating-button"
      :class="{ 'is-active': star <= modelValue }"
      :disabled="readonly"
      :aria-label="`おすすめ度 ${star} を選択`"
      @click="selectRating(star)"
    >
      <Star :size="size" :fill="star <= modelValue ? 'currentColor' : 'none'" />
    </button>
  </div>
</template>
