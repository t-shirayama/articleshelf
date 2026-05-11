import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { ApiRequestError } from '../../../shared/api/client'
import { useArticleActions } from './useArticleActions'
import type { Article, ArticleInput } from '../types'

describe('useArticleActions', () => {
  it('creates an article and returns to the list', async () => {
    const store = createStore()
    const rotateMotivation = vi.fn()
    const onCreateSuccess = vi.fn()
    const options = createOptions({ store, rotateMotivation, onCreateSuccess })
    const actions = useArticleActions(options)
    const input = articleInput()

    await actions.createArticle(input)

    expect(store.createArticle).toHaveBeenCalledWith(input)
    expect(rotateMotivation).toHaveBeenCalled()
    expect(onCreateSuccess).toHaveBeenCalledWith(createArticle())
    expect(options.modalOpen.value).toBe(false)
    expect(options.viewMode.value).toBe('list')
    expect(actions.isCreatingArticle.value).toBe(false)
  })

  it('surfaces duplicate article ids from API request errors', async () => {
    const store = createStore()
    store.createArticle.mockRejectedValueOnce(new ApiRequestError('Duplicate', 'article-2'))
    const options = createOptions({ store })
    const actions = useArticleActions(options)

    await actions.createArticle(articleInput())

    expect(options.articleFormError.value).toBe('Duplicate')
    expect(options.duplicateArticleId.value).toBe('article-2')
    expect(options.modalOpen.value).toBe(true)
  })

  it('saves and deletes detail articles with guarded loading flags', async () => {
    const store = createStore()
    const navigateToList = vi.fn()
    const options = createOptions({ store, navigateToList })
    options.detailHasUnsavedChanges.value = true
    const actions = useArticleActions(options)

    await actions.saveArticle(articleInput({ id: 'article-1' }))
    await actions.deleteArticle('article-1')

    expect(store.updateArticle).toHaveBeenCalledWith(articleInput({ id: 'article-1' }))
    expect(store.deleteArticle).toHaveBeenCalledWith('article-1')
    expect(options.detailHasUnsavedChanges.value).toBe(false)
    expect(navigateToList).toHaveBeenCalled()
    expect(actions.isSavingDetail.value).toBe(false)
    expect(actions.isDeletingArticle.value).toBe(false)
  })

  it('handles list deletion and open detail failures', async () => {
    const store = createStore()
    store.deleteArticle.mockRejectedValueOnce(new Error('delete failed'))
    store.selectArticle.mockRejectedValueOnce(new Error('fetch failed'))
    const options = createOptions({ store })
    const actions = useArticleActions(options)
    const article = createArticle()

    actions.requestDeleteArticle(article)
    expect(actions.deleteCandidate.value).toEqual(article)

    await actions.confirmListDelete()
    await actions.openArticle(article)

    expect(actions.deleteCandidate.value).toBeNull()
    expect(store.error).toBe('fetch failed')
  })

  it('opens duplicate articles and delegates favorite changes', async () => {
    const store = createStore()
    const closeForDuplicateOpen = vi.fn()
    const rotateMotivation = vi.fn()
    const options = createOptions({ store, closeForDuplicateOpen, rotateMotivation })
    const actions = useArticleActions(options)
    const article = createArticle()

    await actions.toggleFavorite(article)
    await actions.openDuplicateArticle('article-2')

    expect(store.toggleFavorite).toHaveBeenCalledWith(article)
    expect(closeForDuplicateOpen).toHaveBeenCalled()
    expect(store.selectArticleById).toHaveBeenCalledWith('article-2')
    expect(rotateMotivation).toHaveBeenCalled()
    expect(options.viewMode.value).toBe('detail')
  })
})

type ArticleActionsOptions = Parameters<typeof useArticleActions>[0]
type MockArticleStore = ReturnType<typeof createStore>
type ArticleActionsOverrides = Partial<Omit<ArticleActionsOptions, 'store'>> & {
  store?: MockArticleStore | ArticleActionsOptions['store']
}

function createOptions(overrides: ArticleActionsOverrides = {}): ArticleActionsOptions {
  const { store, ...rest } = overrides
  return {
    store: (store ?? createStore()) as unknown as ArticleActionsOptions['store'],
    t: (key: string) => key,
    viewMode: ref<'list' | 'calendar' | 'detail' | 'tags'>('list'),
    detailHasUnsavedChanges: ref(false),
    articleFormError: ref(''),
    detailFormError: ref(''),
    duplicateArticleId: ref(''),
    modalOpen: ref(true),
    rotateMotivation: vi.fn(),
    navigateToList: vi.fn(),
    closeForDuplicateOpen: vi.fn(),
    ...rest
  }
}

function createStore() {
  return {
    error: '',
    createArticle: vi.fn().mockResolvedValue(createArticle()),
    updateArticle: vi.fn().mockResolvedValue(undefined),
    deleteArticle: vi.fn().mockResolvedValue(undefined),
    selectArticle: vi.fn().mockResolvedValue(undefined),
    toggleFavorite: vi.fn().mockResolvedValue(undefined),
    selectArticleById: vi.fn().mockResolvedValue(undefined)
  }
}

function articleInput(overrides: Partial<ArticleInput> = {}): ArticleInput {
  return {
    id: undefined,
    url: 'https://example.com',
    title: 'Article',
    summary: '',
    status: 'UNREAD',
    readDate: null,
    favorite: false,
    rating: 0,
    notes: '',
    tags: [],
    ...overrides
  }
}

function createArticle(): Article {
  return {
    id: 'article-1',
    url: 'https://example.com',
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
