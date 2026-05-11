import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref, nextTick } from 'vue'
import { ApiRequestError } from '../../../shared/api/client'
import { articlesApi } from '../api/articlesApi'
import { useArticlePreview } from './useArticlePreview'
import type { ArticlePreview } from '../types'

vi.mock('../api/articlesApi', () => ({
  articlesApi: {
    previewArticle: vi.fn()
  }
}))

describe('useArticlePreview', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('resets preview state when the URL changes', async () => {
    const url = ref('https://example.com/a')
    vi.mocked(articlesApi.previewArticle).mockResolvedValueOnce(preview({ url: url.value }))
    const state = useArticlePreview(url, (key) => key)

    await state.requestPreview()
    expect(state.preview.value?.url).toBe('https://example.com/a')

    url.value = 'https://example.com/b'
    await nextTick()

    expect(state.preview.value).toBeNull()
    expect(state.saveDisabledByPreview.value).toBe(false)
  })

  it('does not leak a preview response after reset', async () => {
    const url = ref('https://example.com/slow')
    let resolvePreview: (value: ArticlePreview) => void = () => undefined
    vi.mocked(articlesApi.previewArticle).mockReturnValueOnce(new Promise((resolve) => {
      resolvePreview = resolve
    }))
    const state = useArticlePreview(url, (key) => key)

    const request = state.requestPreview()
    state.reset()
    resolvePreview(preview({ url: url.value }))
    await request

    expect(state.preview.value).toBeNull()
  })

  it('ignores older preview responses during races', async () => {
    const url = ref('https://example.com/a')
    let resolveA: (value: ArticlePreview) => void = () => undefined
    let resolveB: (value: ArticlePreview) => void = () => undefined
    vi.mocked(articlesApi.previewArticle)
      .mockReturnValueOnce(new Promise((resolve) => { resolveA = resolve }))
      .mockReturnValueOnce(new Promise((resolve) => { resolveB = resolve }))
    const state = useArticlePreview(url, (key) => key)

    const requestA = state.requestPreview()
    url.value = 'https://example.com/b'
    await nextTick()
    const requestB = state.requestPreview()
    resolveB(preview({ url: 'https://example.com/b', title: 'B' }))
    await requestB
    resolveA(preview({ url: 'https://example.com/a', title: 'A' }))
    await requestA

    expect(state.preview.value?.url).toBe('https://example.com/b')
    expect(state.preview.value?.title).toBe('B')
  })

  it('disables save for duplicate previews and resets when URL changes', async () => {
    const url = ref('https://example.com/duplicate')
    vi.mocked(articlesApi.previewArticle).mockRejectedValueOnce(
      new ApiRequestError('Duplicate', 'article-1', 409)
    )
    const state = useArticlePreview(url, (key) => key)

    await state.requestPreview()

    expect(state.duplicateArticleId.value).toBe('article-1')
    expect(state.saveDisabledByPreview.value).toBe(true)

    url.value = 'https://example.com/new'
    await nextTick()

    expect(state.duplicateArticleId.value).toBe('')
    expect(state.saveDisabledByPreview.value).toBe(false)
  })

  it('skips preview requests for syntactically invalid or unsupported URLs', async () => {
    const url = ref('not-a-url')
    const state = useArticlePreview(url, (key) => key)

    await state.requestPreview()

    expect(articlesApi.previewArticle).not.toHaveBeenCalled()
    expect(state.previewError.value).toBe('articleForm.validation.urlInvalid')
    expect(state.loading.value).toBe(false)

    url.value = 'ftp://example.com/article'
    await nextTick()
    await state.requestPreview()

    expect(articlesApi.previewArticle).not.toHaveBeenCalled()
    expect(state.previewError.value).toBe('articleForm.validation.urlInvalid')
  })
})

function preview(overrides: Partial<ArticlePreview> = {}): ArticlePreview {
  return {
    url: 'https://example.com',
    title: 'Title',
    summary: 'Summary',
    thumbnailUrl: '',
    previewAvailable: true,
    errorReason: null,
    ...overrides
  }
}
