import { describe, expect, it } from 'vitest'
import { replaceArticle, toArticleInput } from './articleMappers'
import type { Article } from '../types'

describe('articleMappers', () => {
  it('replaces only the matching article', () => {
    const articles = [
      article({ id: 'a1', title: 'Old' }),
      article({ id: 'a2', title: 'Keep' })
    ]
    const updated = article({ id: 'a1', title: 'New' })

    expect(replaceArticle(articles, updated)).toEqual([
      updated,
      articles[1]
    ])
  })

  it('maps nullable article fields into editable input defaults', () => {
    expect(toArticleInput(article({
      summary: null,
      readDate: undefined,
      notes: null,
      tags: [{ name: 'Vue' }, { name: '' }]
    }))).toMatchObject({
      summary: '',
      readDate: null,
      notes: '',
      tags: ['Vue']
    })
  })
})

function article(overrides: Partial<Article> = {}): Article {
  return {
    id: 'article',
    version: 0,
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
