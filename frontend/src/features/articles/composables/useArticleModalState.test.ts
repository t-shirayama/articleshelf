import { describe, expect, it } from 'vitest'
import { useArticleModalState } from './useArticleModalState'

describe('useArticleModalState', () => {
  it('opens and resets article modal errors', () => {
    const state = useArticleModalState()
    state.articleFormError.value = 'error'
    state.duplicateArticleId.value = 'article-1'

    state.openArticleModal({ url: 'https://example.com', title: 'Example' })

    expect(state.modalOpen.value).toBe(true)
    expect(state.articleFormError.value).toBe('')
    expect(state.duplicateArticleId.value).toBe('')
    expect(state.articleFormSeed.value).toEqual({
      url: 'https://example.com',
      title: 'Example'
    })
  })

  it('does not close while an article is being created', () => {
    const state = useArticleModalState()
    state.modalOpen.value = true
    state.articleFormError.value = 'error'

    state.closeArticleModal(true)

    expect(state.modalOpen.value).toBe(true)
    expect(state.articleFormError.value).toBe('error')
  })

  it('closes for duplicate navigation', () => {
    const state = useArticleModalState()
    state.modalOpen.value = true
    state.articleFormError.value = 'error'
    state.duplicateArticleId.value = 'article-1'

    state.closeForDuplicateOpen()

    expect(state.modalOpen.value).toBe(false)
    expect(state.articleFormError.value).toBe('')
    expect(state.duplicateArticleId.value).toBe('')
    expect(state.articleFormSeed.value).toBeNull()
  })
})
