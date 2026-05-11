import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getMotivationCards } from '../data/motivationCards'

export function useMotivationRotation() {
  const { locale } = useI18n()
  const motivationCards = computed(() => getMotivationCards(locale.value))
  const motivationIndex = ref(randomMotivationIndex(motivationCards.value.length))
  const currentMotivation = computed(() => motivationCards.value[motivationIndex.value % motivationCards.value.length])

  function rotateMotivation(): void {
    motivationIndex.value = randomMotivationIndex(motivationCards.value.length, motivationIndex.value)
  }

  return {
    currentMotivation,
    rotateMotivation
  }
}

function randomMotivationIndex(length = 1, currentIndex = -1): number {
  if (length <= 1) return 0

  let nextIndex = currentIndex
  while (nextIndex === currentIndex) {
    nextIndex = Math.floor(Math.random() * length)
  }
  return nextIndex
}
