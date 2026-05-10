import { onBeforeUnmount, type Ref, watch } from 'vue'

export function useArticleSearchDebounce(
  searchDraft: Ref<string>,
  applySearch: (value: string) => void,
  delayMs = 250
): { cancelSearch: () => void, flushSearch: () => void } {
  let searchTimer: ReturnType<typeof window.setTimeout> | undefined

  function cancelSearch(): void {
    if (!searchTimer) return
    window.clearTimeout(searchTimer)
    searchTimer = undefined
  }

  function flushSearch(): void {
    cancelSearch()
    applySearch(searchDraft.value)
  }

  const stop = watch(searchDraft, (value) => {
    cancelSearch()
    searchTimer = window.setTimeout(() => {
      searchTimer = undefined
      applySearch(value)
    }, delayMs)
  })

  onBeforeUnmount(() => {
    cancelSearch()
    stop()
  })

  return { cancelSearch, flushSearch }
}
