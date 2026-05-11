import { describe, expect, it } from 'vitest'
import { createFormToArticleInput, hasArticleDetailFormChanges, normalizeTagNames, type ArticleDetailForm } from './articleForms'
import type { Article } from '../types'

describe('articleForms', () => {
  it('creates unread input when readLater is checked', () => {
    const input = createFormToArticleInput({
      url: ' https://example.com/read-later ',
      title: 'Read later',
      summary: '',
      readLater: true,
      readDate: '2026-05-07',
      favorite: false,
      rating: 0,
      notes: '',
      tags: [' Vue ', '', 'Vue']
    })

    expect(input).toMatchObject({
      url: 'https://example.com/read-later',
      status: 'UNREAD',
      readDate: null,
      tags: ['Vue']
    })
  })

  it('keeps read date when creating a read article', () => {
    const input = createFormToArticleInput({
      url: 'https://example.com/read',
      title: 'Read',
      summary: '',
      readLater: false,
      readDate: '2026-05-07',
      favorite: true,
      rating: 4,
      notes: 'memo',
      tags: []
    })

    expect(input.status).toBe('READ')
    expect(input.readDate).toBe('2026-05-07')
  })

  it('omits blank title and summary from create input', () => {
    const input = createFormToArticleInput({
      url: 'https://example.com/no-details',
      title: '   ',
      summary: '',
      readLater: true,
      readDate: null,
      favorite: false,
      rating: 0,
      notes: '',
      tags: []
    })

    expect('title' in input).toBe(false)
    expect('summary' in input).toBe(false)
  })

  it('detects changed detail form and normalizes tags', () => {
    const article: Article = {
      id: 'a1',
      url: 'https://example.com',
      title: 'Title',
      summary: '',
      status: 'READ',
      readDate: '2026-05-07',
      favorite: false,
      rating: 3,
      notes: 'memo',
      tags: [{ name: 'Vue' }],
      createdAt: '2026-05-01T00:00:00Z',
      updatedAt: '2026-05-01T00:00:00Z'
    }
    const form: ArticleDetailForm = {
      id: 'a1',
      url: 'https://example.com',
      title: 'Title',
      summary: '',
      status: 'READ',
      readDate: '2026-05-07',
      favorite: false,
      rating: 3,
      notes: 'updated memo',
      tags: ['Vue']
    }

    expect(normalizeTagNames([' Vue ', '', 'Java', 'Vue'])).toEqual(['Vue', 'Java'])
    expect(hasArticleDetailFormChanges(form, article)).toBe(true)
  })
})
