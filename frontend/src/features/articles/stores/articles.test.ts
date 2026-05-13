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
    previewArticle: vi.fn(),
    updateArticle: vi.fn(),
    deleteArticle: vi.fn(),
    findTags: vi.fn(),
    createTag: vi.fn(),
    renameTag: vi.fn(),
    mergeTag: vi.fn(),
    deleteTag: vi.fn()
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

  it('fetches articles and keeps selected article in sync', async () => {
    const store = useArticlesStore()
    store.selectedArticle = article({ id: 'a1', title: 'Old' })
    vi.mocked(articlesApi.findArticles).mockResolvedValueOnce([
      article({ id: 'a1', title: 'Updated' }),
      article({ id: 'a2', title: 'Other' })
    ])

    await store.fetchArticles()

    expect(store.loading).toBe(false)
    expect(store.error).toBe('')
    expect(store.articles.map((item) => item.id)).toEqual(['a1', 'a2'])
    expect(store.selectedArticle?.title).toBe('Updated')
  })

  it('handles fetch failures with a translated error fallback', async () => {
    const store = useArticlesStore()
    vi.mocked(articlesApi.findArticles).mockRejectedValueOnce(new Error('network down'))

    await store.fetchArticles()

    expect(store.loading).toBe(false)
    expect(store.error).toBe('network down')
  })

  it('creates, updates, deletes, and selects articles through the API', async () => {
    const store = useArticlesStore()
    const created = article({ id: 'created' })
    const updated = article({ id: 'created', title: 'Updated' })
    vi.mocked(articlesApi.createArticle).mockResolvedValueOnce(created)
    vi.mocked(articlesApi.updateArticle).mockResolvedValueOnce(updated)
    vi.mocked(articlesApi.deleteArticle).mockResolvedValueOnce(null)
    vi.mocked(articlesApi.findArticle).mockResolvedValueOnce(created).mockResolvedValueOnce(updated)
    vi.mocked(articlesApi.findTags).mockResolvedValue([])
    vi.mocked(articlesApi.findArticles).mockResolvedValue([])

    await store.createArticle({ ...created, tags: [] })
    await store.updateArticle({ ...updated, tags: [], id: 'created' })
    await store.selectArticle(created)
    await store.selectArticleById('created')
    await store.deleteArticle('created')

    expect(articlesApi.createArticle).toHaveBeenCalled()
    expect(articlesApi.updateArticle).toHaveBeenCalledWith('created', expect.objectContaining({ title: 'Updated' }))
    expect(articlesApi.findArticle).toHaveBeenCalledWith('created')
    expect(articlesApi.deleteArticle).toHaveBeenCalledWith('created')
    expect(store.selectedArticle).toBeNull()
  })

  it('updates tag data and article data after tag mutations', async () => {
    const store = useArticlesStore()
    vi.mocked(articlesApi.createTag).mockResolvedValue({ id: 't1', name: 'Vue' })
    vi.mocked(articlesApi.renameTag).mockResolvedValue({ id: 't1', name: 'Testing' })
    vi.mocked(articlesApi.mergeTag).mockResolvedValue(null)
    vi.mocked(articlesApi.deleteTag).mockResolvedValue(null)
    vi.mocked(articlesApi.findTags).mockResolvedValue([{ id: 't1', name: 'Testing' }])
    vi.mocked(articlesApi.findArticles).mockResolvedValue([])

    await store.createTag('Vue')
    await store.renameTag('t1', 'Testing')
    await store.mergeTag('t1', 't2')
    await store.deleteTag('t1')

    expect(articlesApi.findTags).toHaveBeenCalledTimes(4)
    expect(articlesApi.findArticles).toHaveBeenCalledTimes(6)
  })

  it('updates filters and derived counts', async () => {
    const store = useArticlesStore()
    store.articles = [
      article({ id: 'a1', status: 'UNREAD', favorite: true, rating: 5, tags: [{ name: 'Vue' }] }),
      article({ id: 'a2', status: 'READ', favorite: false, rating: 3, tags: [{ name: 'Java' }] })
    ]
    store.articleSnapshot = [...store.articles]

    await store.setFavoriteOnly()
    store.setTags(['Vue'])
    store.setRatings([5, 3])
    store.setCreatedRange({ from: '2026-05-01', to: '2026-05-31' })
    store.setReadRange({ from: '2026-05-01', to: '' })
    store.setSearch('article')
    store.setSort('TITLE_ASC')

    expect(store.counts).toEqual({ all: 2, unread: 1, read: 1, favorite: 1 })
    expect(store.filters).toMatchObject({
      favorite: true,
      tags: ['Vue'],
      ratings: [3, 5],
      createdRange: { from: '2026-05-01', to: '2026-05-31' },
      readRange: { from: '2026-05-01', to: '' },
      search: 'article',
      sort: 'TITLE_ASC'
    })

    await store.setStatus('READ')
    expect(store.filters.favorite).toBe(false)

    await store.setAllArticles()
    expect(store.filters.status).toBe('ALL')
    expect(store.filters.tags).toEqual([])
  })
})

function article(overrides: Partial<Article>): Article {
  return {
    id: 'article',
    version: 0,
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
