import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '../../../shared/api/client'
import { articlesApi } from './articlesApi'
import type { ArticleFilters, ArticleInput } from '../types'

vi.mock('../../../shared/api/client', () => ({
  request: vi.fn()
}))

describe('articlesApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(request).mockResolvedValue(null)
  })

  it('serializes article list filters into query parameters', async () => {
    await articlesApi.findArticles(filters({
      status: 'READ',
      search: 'vue testing',
      favorite: true
    }))

    expect(request).toHaveBeenCalledWith('/api/articles?status=READ&search=vue+testing&favorite=true')
  })

  it('omits default article list filters from query parameters', async () => {
    await articlesApi.findArticles(filters())

    expect(request).toHaveBeenCalledWith('/api/articles')
  })

  it('uses the expected article and tag endpoints', async () => {
    const input: ArticleInput = {
      url: 'https://example.com',
      title: 'Title',
      summary: '',
      status: 'UNREAD',
      readDate: null,
      favorite: false,
      rating: 0,
      notes: '',
      tags: ['Vue']
    }

    await articlesApi.findArticle('a1')
    await articlesApi.createArticle(input)
    await articlesApi.previewArticle('https://example.com')
    await articlesApi.updateArticle('a1', input)
    await articlesApi.deleteArticle('a1')
    await articlesApi.findTags()
    await articlesApi.createTag('Vue')
    await articlesApi.renameTag('t1', 'Testing')
    await articlesApi.mergeTag('t1', 't2')
    await articlesApi.deleteTag('t1')

    expect(vi.mocked(request).mock.calls).toEqual([
      ['/api/articles/a1'],
      ['/api/articles', { method: 'POST', body: JSON.stringify(input) }],
      ['/api/articles/preview', { method: 'POST', body: JSON.stringify({ url: 'https://example.com' }) }],
      ['/api/articles/a1', { method: 'PUT', body: JSON.stringify(input) }],
      ['/api/articles/a1', { method: 'DELETE' }],
      ['/api/tags'],
      ['/api/tags', { method: 'POST', body: JSON.stringify({ name: 'Vue' }) }],
      ['/api/tags/t1', { method: 'PATCH', body: JSON.stringify({ name: 'Testing' }) }],
      ['/api/tags/t1/merge', { method: 'POST', body: JSON.stringify({ targetTagId: 't2' }) }],
      ['/api/tags/t1', { method: 'DELETE' }]
    ])
  })
})

function filters(overrides: Partial<ArticleFilters> = {}): ArticleFilters {
  return {
    status: 'ALL',
    tags: [],
    ratings: [],
    createdRange: { from: '', to: '' },
    readRange: { from: '', to: '' },
    search: '',
    favorite: false,
    sort: 'CREATED_DESC',
    ...overrides
  }
}
