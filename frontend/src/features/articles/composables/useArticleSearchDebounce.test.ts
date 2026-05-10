import { describe, expect, it, vi } from 'vitest'
import { createApp, ref } from 'vue'
import { useArticleSearchDebounce } from './useArticleSearchDebounce'

describe('useArticleSearchDebounce', () => {
  it('applies the latest search value after the debounce delay', async () => {
    vi.useFakeTimers()
    const searchDraft = ref('')
    const applySearch = vi.fn()
    const app = mountComposable(() => useArticleSearchDebounce(searchDraft, applySearch, 250))

    searchDraft.value = 'vue'
    await Promise.resolve()
    searchDraft.value = 'vue pinia'
    await Promise.resolve()

    vi.advanceTimersByTime(249)
    expect(applySearch).not.toHaveBeenCalled()

    vi.advanceTimersByTime(1)
    expect(applySearch).toHaveBeenCalledWith('vue pinia')

    app.unmount()
    vi.useRealTimers()
  })

  it('cancels pending search when the owner unmounts', async () => {
    vi.useFakeTimers()
    const searchDraft = ref('')
    const applySearch = vi.fn()
    const app = mountComposable(() => useArticleSearchDebounce(searchDraft, applySearch, 250))

    searchDraft.value = 'stale'
    await Promise.resolve()
    app.unmount()
    vi.advanceTimersByTime(250)

    expect(applySearch).not.toHaveBeenCalled()
    vi.useRealTimers()
  })
})

function mountComposable(useComposable: () => void) {
  const root = document.createElement('div')
  const app = createApp({
    setup() {
      useComposable()
      return {}
    },
    template: '<div />'
  })
  app.mount(root)
  return app
}
