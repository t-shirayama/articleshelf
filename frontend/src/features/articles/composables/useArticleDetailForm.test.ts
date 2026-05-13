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

  it('resets editing state when the selected article changes', async () => {
    const selected = ref(article({ title: 'First' }))
    const form = useArticleDetailForm(selected, ref([]), (key) => key, vi.fn())

    form.detailMode.value = 'edit'
    form.articleDetailsOpen.value = true
    form.submitted.value = true
    selected.value = article({ id: 'article-2', title: 'Second', url: 'https://example.com/second' })
    await nextTick()

    expect(form.form.title).toBe('Second')
    expect(form.isEditing.value).toBe(false)
    expect(form.articleDetailsOpen.value).toBe(false)
    expect(form.submitted.value).toBe(false)
  })

  it('validates required fields and returns normalized detail input', () => {
    const form = useArticleDetailForm(ref(article()), ref([]), (key) => key, vi.fn())

    form.detailMode.value = 'edit'
    form.form.title = ''
    expect(form.createSubmitInput(false)).toBeNull()
    expect(form.titleError.value).toBe('detail.titleRequired')

    form.form.title = ' Updated title '
    form.form.url = ' https://example.com/updated '
    form.form.status = 'READ'
    form.form.readDate = ''
    expect(form.createSubmitInput(false)).toBeNull()
    expect(form.readDateError.value).toBe('detail.readDateRequired')

    form.form.readDate = '2026-05-11'
    form.form.tags = [' Vue ', 'Vue', '']
    expect(form.createSubmitInput(false)).toMatchObject({
      title: ' Updated title ',
      url: ' https://example.com/updated ',
      status: 'READ',
      readDate: '2026-05-11',
      tags: ['Vue'],
    })
  })

  it('toggles favorite directly only when there are no unsaved edits', () => {
    const baseArticle = article({ favorite: false })
    const form = useArticleDetailForm(ref(baseArticle), ref([]), (key) => key, vi.fn())

    expect(form.createFavoriteInput()).toMatchObject({ favorite: true })

    form.detailMode.value = 'edit'
    expect(form.createFavoriteInput()).toBeNull()
    expect(form.form.favorite).toBe(true)
  })

  it('reports dirty state and exposes fallback summary and notes text', async () => {
    const onDirtyChange = vi.fn()
    const form = useArticleDetailForm(ref(article({ summary: '', notes: '' })), ref([]), (key) => key, onDirtyChange)

    expect(form.summaryText.value).toBe('detail.emptySummary')
    expect(form.notesText.value).toBe('detail.emptyNotes')

    form.form.notes = 'Remember this'
    await nextTick()

    expect(form.notesText.value).toBe('Remember this')
    expect(onDirtyChange).toHaveBeenLastCalledWith(true)
  })
})

function article(overrides: Partial<Article> = {}): Article {
  return {
    id: 'article-1',
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
    ...overrides,
  }
}
