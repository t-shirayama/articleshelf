import { afterEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import { useArticleDetailForm } from './useArticleDetailForm'
import type { Article } from '../types'

describe('useArticleDetailForm', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('sets today as read date when detail status changes to read without a read date', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-10T12:00:00'))

    const form = useArticleDetailForm(ref(article({ status: 'UNREAD', readDate: null })), ref([]), (key) => key, vi.fn())

    form.detailMode.value = 'edit'
    form.form.status = 'READ'
    await nextTick()

    expect(form.form.readDate).toBe('2026-05-10')
  })

  it('clears read date when detail status changes to unread', async () => {
    const form = useArticleDetailForm(ref(article({ status: 'READ', readDate: '2026-05-07' })), ref([]), (key) => key, vi.fn())

    form.detailMode.value = 'edit'
    form.form.status = 'UNREAD'
    await nextTick()

    expect(form.form.readDate).toBeNull()
  })

  it('keeps an existing read date when detail status changes to read', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-10T12:00:00'))

    const form = useArticleDetailForm(ref(article({ status: 'UNREAD', readDate: null })), ref([]), (key) => key, vi.fn())

    form.detailMode.value = 'edit'
    form.form.readDate = '2026-05-07'
    form.form.status = 'READ'
    await nextTick()

    expect(form.form.readDate).toBe('2026-05-07')
  })
})

function article(overrides: Partial<Article> = {}): Article {
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
    updatedAt: '2026-05-01T00:00:00Z',
    ...overrides,
  }
}
