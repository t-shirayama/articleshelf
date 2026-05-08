import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { useArticleActions } from './useArticleActions'
import type { useArticlesStore } from '../stores/articles'
import type { Article } from '../types'

type ArticlesStore = ReturnType<typeof useArticlesStore>

describe('useArticleActions error handling', () => {
  it('keeps list delete failures inside the list error banner', async () => {
    const store = {
      error: '',
      deleteArticle: vi.fn().mockRejectedValue(new Error('Could not delete on API'))
    } as unknown as ArticlesStore
    const actions = useArticleActions({
      store,
      t: (key) => key,
      viewMode: ref('list'),
      detailHasUnsavedChanges: ref(false),
      articleFormError: ref(''),
      detailFormError: ref(''),
      duplicateArticleId: ref(''),
      modalOpen: ref(false),
      rotateMotivation: vi.fn(),
      navigateToList: vi.fn(),
      closeForDuplicateOpen: vi.fn()
    })

    actions.requestDeleteArticle(article())
    await actions.confirmListDelete()

    expect(store.error).toBe('Could not delete on API')
    expect(actions.deleteCandidate.value).toBeNull()
  })

  it('keeps stale article open failures on the list instead of navigating to detail', async () => {
    const viewMode = ref<'list' | 'calendar' | 'detail' | 'tags'>('list')
    const rotateMotivation = vi.fn()
    const store = {
      error: '',
      selectArticle: vi.fn().mockRejectedValue(new Error('Article was not found'))
    } as unknown as ArticlesStore
    const actions = useArticleActions({
      store,
      t: (key) => key,
      viewMode,
      detailHasUnsavedChanges: ref(false),
      articleFormError: ref(''),
      detailFormError: ref(''),
      duplicateArticleId: ref(''),
      modalOpen: ref(false),
      rotateMotivation,
      navigateToList: vi.fn(),
      closeForDuplicateOpen: vi.fn()
    })

    await actions.openArticle(article())

    expect(store.error).toBe('Article was not found')
    expect(viewMode.value).toBe('list')
    expect(rotateMotivation).not.toHaveBeenCalled()
  })
})

function article(): Article {
  return {
    id: 'article-1',
    url: 'https://example.com/article',
    title: 'Article',
    summary: '',
    thumbnailUrl: '',
    status: 'UNREAD',
    readDate: null,
    favorite: false,
    rating: 0,
    notes: '',
    tags: [],
    createdAt: '2026-05-01T00:00:00Z',
    updatedAt: '2026-05-01T00:00:00Z'
  }
}
