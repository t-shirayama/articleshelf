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

  it('returns every article when the trimmed search keyword is empty', () => {
    const filters = createDefaultArticleFilters()
    filters.search = '   '

    const result = filterArticles([
      article({ id: 'first' }),
      article({ id: 'second', title: 'Another Article' })
    ], filters)

    expect(result.map((item) => item.id)).toEqual(['first', 'second'])
  })

  it('filters by date ranges and rejects articles with missing read dates', () => {
    const filters = createDefaultArticleFilters()
    filters.createdRange = { from: '2026-05-02', to: '2026-05-03' }
    filters.readRange = { from: '2026-05-10', to: '2026-05-11' }

    const result = filterArticles([
      article({ id: 'match', createdAt: '2026-05-02T09:00:00Z', readDate: '2026-05-10' }),
      article({ id: 'too-early', createdAt: '2026-05-01T09:00:00Z', readDate: '2026-05-10' }),
      article({ id: 'too-late', createdAt: '2026-05-03T09:00:00Z', readDate: '2026-05-12' }),
      article({ id: 'missing-read-date', createdAt: '2026-05-02T09:00:00Z', readDate: null })
    ], filters)

    expect(result.map((item) => item.id)).toEqual(['match'])
  })

  it('searches notes, tags and url case-insensitively', () => {
    const filters = createDefaultArticleFilters()
    filters.search = 'SECURITY'

    const result = filterArticles([
      article({ id: 'url-match', url: 'https://example.com/security' }),
      article({ id: 'notes-match', notes: 'security checklist' }),
      article({ id: 'tags-match', tags: [{ name: 'Security' }] }),
      article({ id: 'miss', title: 'Observability' })
    ], filters)

    expect(result.map((item) => item.id)).toEqual(['url-match', 'notes-match', 'tags-match'])
  })

  it('sorts by each supported sort option including null-safe date comparisons', () => {
    const baseArticles = [
      article({ id: 'created-old', title: 'Zulu', createdAt: '2026-05-01T00:00:00Z', updatedAt: '2026-05-02T00:00:00Z', readDate: null, rating: 3 }),
      article({ id: 'created-new', title: 'Alpha', createdAt: '2026-05-03T00:00:00Z', updatedAt: '2026-05-04T00:00:00Z', readDate: '2026-05-10', rating: 5 }),
      article({ id: 'updated-new', title: 'Bravo', createdAt: '2026-05-02T00:00:00Z', updatedAt: '2026-05-05T00:00:00Z', readDate: '2026-05-09', rating: 4 }),
      article({ id: 'read-latest', title: 'Charlie', createdAt: '2026-05-02T12:00:00Z', updatedAt: '2026-05-03T00:00:00Z', readDate: '2026-05-11', rating: 2 })
    ]

    expect(sortArticles(baseArticles, 'CREATED_ASC').map((item) => item.id)).toEqual([
      'created-old',
      'updated-new',
      'read-latest',
      'created-new'
    ])
    expect(sortArticles(baseArticles, 'CREATED_DESC').map((item) => item.id)).toEqual([
      'created-new',
      'read-latest',
      'updated-new',
      'created-old'
    ])
    expect(sortArticles(baseArticles, 'UPDATED_DESC').map((item) => item.id)).toEqual([
      'updated-new',
      'created-new',
      'read-latest',
      'created-old'
    ])
    expect(sortArticles(baseArticles, 'READ_DATE_DESC').map((item) => item.id)).toEqual([
      'read-latest',
      'created-new',
      'updated-new',
      'created-old'
    ])
    expect(sortArticles(baseArticles, 'TITLE_ASC').map((item) => item.id)).toEqual([
      'created-new',
      'updated-new',
      'read-latest',
      'created-old'
    ])
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
