import { describe, expect, it } from 'vitest'
import { createCalendarCells, dateKey, monthKeyToDate, toDateKey, toMonthKey } from './calendar'
import type { Article } from '../types'

describe('calendar domain helpers', () => {
  it('creates fixed six-week calendar cells with articles for the selected mode', () => {
    const cells = createCalendarCells(
      new Date(2026, 4, 1),
      [
        article('created', '2026-05-10T12:00:00Z', null),
        article('read', '2026-05-01T12:00:00Z', '2026-05-10')
      ],
      'read'
    )

    expect(cells).toHaveLength(42)
    expect(cells[0].outside).toBe(true)
    expect(cells.find((cell) => cell.label === 1)?.date).toBe('2026-05-01')
    expect(cells.find((cell) => cell.date === '2026-05-10')?.articles.map((item) => item.title))
      .toEqual(['read'])
  })

  it('normalizes date and month keys', () => {
    expect(dateKey('2026-05-10T12:00:00Z')).toBe('2026-05-10')
    expect(toDateKey(new Date(2026, 4, 3))).toBe('2026-05-03')
    expect(toMonthKey(new Date(2026, 4, 15))).toBe('2026-05')
    expect(monthKeyToDate('2026-05').getMonth()).toBe(4)
    expect(monthKeyToDate('bad', new Date(2026, 6, 20)).getMonth()).toBe(6)
  })
})

function article(title: string, createdAt: string, readDate: string | null): Article {
  return {
    id: title,
    url: `https://example.com/${title}`,
    title,
    summary: '',
    thumbnailUrl: '',
    status: readDate ? 'READ' : 'UNREAD',
    readDate,
    favorite: false,
    rating: 0,
    notes: '',
    tags: [],
    createdAt,
    updatedAt: createdAt
  }
}
