<script setup lang="ts">
import { computed, ref } from 'vue'
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

const hoverRating = ref(0)

const previewRating = computed(() => {
  if (props.readonly || hoverRating.value === 0) return props.modelValue
  return hoverRating.value
})

function selectRating(value: number): void {
  if (props.readonly) return
  emit('update:modelValue', value)
}

function preview(value: number): void {
  if (props.readonly) return
  hoverRating.value = value
}

function clearPreview(): void {
  hoverRating.value = 0
}
</script>

<template>
  <div
    class="star-rating"
    :class="{ 'is-readonly': readonly }"
    :aria-label="`おすすめ度 ${modelValue} / 5`"
    @mouseleave="clearPreview"
  >
    <button
      v-for="star in 5"
      :key="star"
      type="button"
      class="star-rating-button"
      :class="{
        'is-active': star <= modelValue,
        'is-preview': !readonly && hoverRating > 0 && star <= hoverRating
      }"
      :disabled="readonly"
      :aria-label="`おすすめ度 ${star} を選択`"
      @focus="preview(star)"
      @blur="clearPreview"
      @mouseenter="preview(star)"
      @click="selectRating(star)"
    >
      <Star :size="size" :fill="star <= previewRating ? 'currentColor' : 'none'" />
    </button>
  </div>
</template>
