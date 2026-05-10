import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useArticlesStore } from './articles'
import { articlesApi } from '../api/articlesApi'
import type { Article } from '../types'

vi.mock('../api/articlesApi', () => ({
  articlesApi: {
    findArticles: vi.fn(),
    findArticle: vi.fn(),
    createArticle: vi.fn(),
    updateArticle: vi.fn(),
    deleteArticle: vi.fn(),
    findTags: vi.fn(),
    createTag: vi.fn()
  }
}))

describe('articles store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('rolls back optimistic favorite update when API update fails', async () => {
    const store = useArticlesStore()
    const original = article({ id: 'a1', favorite: false })
    store.articles = [original]
    store.selectedArticle = original
    vi.mocked(articlesApi.updateArticle).mockRejectedValueOnce(new Error('保存に失敗しました'))

    await store.toggleFavorite(original)

    expect(store.articles[0].favorite).toBe(false)
    expect(store.selectedArticle?.favorite).toBe(false)
    expect(store.error).toBe('保存に失敗しました')
  })

  it('applies optimistic status update when API update succeeds', async () => {
    const store = useArticlesStore()
    const original = article({ id: 'a1', status: 'UNREAD', readDate: null })
    const updated = article({ id: 'a1', status: 'READ', readDate: '2026-05-07' })
    store.articles = [original]
    vi.mocked(articlesApi.updateArticle).mockResolvedValueOnce(updated)

    const result = await store.updateArticleStatus(original, 'READ', '2026-05-07')

    expect(result).toEqual(updated)
    expect(store.articles[0]).toMatchObject({ status: 'READ', readDate: '2026-05-07' })
  })

  it('resets user scoped article state on logout', () => {
    const store = useArticlesStore()
    store.articles = [article({ id: 'a1' })]
    store.tags = [{ name: 'Vue' }]
    store.selectedArticle = article({ id: 'a1' })
    store.error = 'error'

    store.resetState()

    expect(store.articles).toEqual([])
    expect(store.tags).toEqual([])
    expect(store.selectedArticle).toBeNull()
    expect(store.error).toBe('')
  })
})

function article(overrides: Partial<Article>): Article {
  return {
    id: 'article',
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
    updatedAt: '2026-05-01T00:00:00Z',
    ...overrides
  }
}
