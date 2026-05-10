import type { Article } from '../types'

export type CalendarMode = 'created' | 'read'

export interface CalendarCell {
  key: string
  label: number | null
  date: string
  weekday: number | null
  articles: Article[]
  outside: boolean
}

export function createCalendarCells(monthDate: Date, articles: Article[], mode: CalendarMode): CalendarCell[] {
  const year = monthDate.getFullYear()
  const month = monthDate.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const cells: CalendarCell[] = []

  for (let index = 0; index < firstDay.getDay(); index += 1) {
    cells.push(blankCell(`blank-start-${index}`))
  }

  for (let day = 1; day <= lastDay.getDate(); day += 1) {
    const date = new Date(year, month, day)
    const key = toDateKey(date)
    cells.push({
      key,
      label: day,
      date: key,
      weekday: date.getDay(),
      articles: articlesForDate(articles, mode, key),
      outside: false
    })
  }

  while (cells.length < 42) {
    cells.push(blankCell(`blank-end-${cells.length}`))
  }

  return cells
}

export function articleDateKey(article: Article, mode: CalendarMode): string {
  return mode === 'created' ? dateKey(article.createdAt) : dateKey(article.readDate)
}

export function dateKey(value?: string | null): string {
  return value ? value.slice(0, 10) : ''
}

export function isDateKeyInRange(key: string, startKey: string, endKey: string): boolean {
  return Boolean(key) && key >= startKey && key <= endKey
}

export function monthKeyToDate(monthKey: string, fallback = new Date()): Date {
  const [year, month] = monthKey.split('-').map(Number)
  if (!year || !month || month < 1 || month > 12) return startOfMonth(fallback)
  return new Date(year, month - 1, 1)
}

export function toDateKey(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function toMonthKey(date: Date): string {
  const monthStart = startOfMonth(date)
  const year = monthStart.getFullYear()
  const month = String(monthStart.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

function articlesForDate(articles: Article[], mode: CalendarMode, key: string): Article[] {
  return articles.filter((article) => articleDateKey(article, mode) === key)
}

function blankCell(key: string): CalendarCell {
  return {
    key,
    label: null,
    date: '',
    weekday: null,
    articles: [],
    outside: true
  }
}

function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}
