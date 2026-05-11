import { afterEach, describe, expect, it, vi } from 'vitest'
import { useStatusUndo } from './useStatusUndo'
import type { Article } from '../types'

describe('useStatusUndo', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('toggles article status and stores undo state', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-11T10:00:00'))
    const article = createArticle({ status: 'UNREAD', readDate: null })
    const store = createStore([article])
    store.updateArticleStatus.mockResolvedValueOnce({ ...article, status: 'READ', readDate: '2026-05-11' })

    const undo = useStatusUndo(store as unknown as Parameters<typeof useStatusUndo>[0], (key) => key)

    await undo.toggleArticleStatus(article)

    expect(store.updateArticleStatus).toHaveBeenCalledWith(article, 'READ', '2026-05-11')
    expect(undo.statusSnackbarMessage.value).toBe('articles.statusReadDone')
    expect(undo.statusSnackbarOpen.value).toBe(true)
  })

  it('restores the previous status when undoing', async () => {
    const article = createArticle({ status: 'READ', readDate: '2026-05-10' })
    const store = createStore([article])
    store.updateArticleStatus.mockResolvedValueOnce({ ...article, status: 'UNREAD', readDate: null })
    const undo = useStatusUndo(store as unknown as Parameters<typeof useStatusUndo>[0], (key) => key)

    await undo.toggleArticleStatus(article)
    await undo.undoArticleStatus()

    expect(store.updateArticleStatus).toHaveBeenLastCalledWith(article, 'READ', '2026-05-10')
    expect(undo.statusSnackbarOpen.value).toBe(false)
  })

  it('does not expose undo UI when the optimistic update fails', async () => {
    const article = createArticle({ status: 'UNREAD' })
    const store = createStore([article])
    store.updateArticleStatus.mockResolvedValueOnce(null)
    const undo = useStatusUndo(store as unknown as Parameters<typeof useStatusUndo>[0], (key) => key)

    await undo.toggleArticleStatus(article)
    await undo.undoArticleStatus()

    expect(undo.statusSnackbarOpen.value).toBe(false)
    expect(store.updateArticleStatus).toHaveBeenCalledTimes(1)
  })
})

function createStore(articles: Article[]) {
  return {
    articles,
    updateArticleStatus: vi.fn()
  }
}

function createArticle(overrides: Partial<Article> = {}): Article {
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
    updatedAt: '2026-05-01T00:00:00Z',
    ...overrides
  }
}
