import { describe, expect, it } from 'vitest'
import { createDefaultArticleFilters, filterArticles, sortArticles } from './articleFilters'
import type { Article } from '../types'

describe('articleFilters', () => {
  it('filters by status, favorite, tag, rating, date range and search keyword', () => {
    const filters = createDefaultArticleFilters()
    filters.status = 'READ'
    filters.favorite = true
    filters.tags = ['Vue']
    filters.ratings = [5]
    filters.createdRange = { from: '2026-05-01', to: '2026-05-31' }
    filters.readRange = { from: '2026-05-07', to: '2026-05-07' }
    filters.search = 'pinia'

    const result = filterArticles([
      article({ id: 'match', title: 'Vue Pinia', tags: [{ name: 'Vue' }], rating: 5, favorite: true, status: 'READ', readDate: '2026-05-07', createdAt: '2026-05-01T00:00:00Z' }),
      article({ id: 'unread', title: 'Vue Pinia', tags: [{ name: 'Vue' }], rating: 5, favorite: true, status: 'UNREAD', readDate: null, createdAt: '2026-05-01T00:00:00Z' }),
      article({ id: 'tag', title: 'Vue Pinia', tags: [{ name: 'Java' }], rating: 5, favorite: true, status: 'READ', readDate: '2026-05-07', createdAt: '2026-05-01T00:00:00Z' })
    ], filters)

    expect(result.map((item) => item.id)).toEqual(['match'])
  })

  it('sorts by rating and falls back to updated date', () => {
    const result = sortArticles([
      article({ id: 'old-five', rating: 5, updatedAt: '2026-05-01T00:00:00Z' }),
      article({ id: 'new-five', rating: 5, updatedAt: '2026-05-03T00:00:00Z' }),
      article({ id: 'four', rating: 4, updatedAt: '2026-05-05T00:00:00Z' })
    ], 'RATING_DESC')

    expect(result.map((item) => item.id)).toEqual(['new-five', 'old-five', 'four'])
  })

  it('searches summary to match the backend search contract', () => {
    const filters = createDefaultArticleFilters()
    filters.search = 'ogp'

    const result = filterArticles([
      article({ id: 'match', summary: 'OGP parser notes' }),
      article({ id: 'miss', summary: 'Markdown parser notes' })
    ], filters)

    expect(result.map((item) => item.id)).toEqual(['match'])
  })

  it('matches any selected tag as an OR filter', () => {
    const filters = createDefaultArticleFilters()
    filters.tags = ['Vue', 'Java']

    const result = filterArticles([
      article({ id: 'vue', tags: [{ name: 'Vue' }] }),
      article({ id: 'java', tags: [{ name: 'Java' }] }),
      article({ id: 'css', tags: [{ name: 'CSS' }] })
    ], filters)

    expect(result.map((item) => item.id)).toEqual(['vue', 'java'])
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
