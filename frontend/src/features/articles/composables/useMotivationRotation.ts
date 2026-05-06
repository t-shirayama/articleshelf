import { computed, ref } from 'vue'
import { motivationCards } from '../data/motivationCards'

export function useMotivationRotation() {
  const motivationIndex = ref(randomMotivationIndex())
  const currentMotivation = computed(() => motivationCards[motivationIndex.value])

  function rotateMotivation(): void {
    motivationIndex.value = randomMotivationIndex(motivationIndex.value)
  }

  return {
    currentMotivation,
    rotateMotivation
  }
}

function randomMotivationIndex(currentIndex = -1): number {
  if (motivationCards.length <= 1) return 0

  let nextIndex = currentIndex
  while (nextIndex === currentIndex) {
    nextIndex = Math.floor(Math.random() * motivationCards.length)
  }
  return nextIndex
}
