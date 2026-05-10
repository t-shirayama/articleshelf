import type { ArticleStatus } from '../types'

type PersistedArticleStatus = Exclude<ArticleStatus, 'ALL'>

export function readDateForStatus(
  status: PersistedArticleStatus,
  currentReadDate: string | null | undefined,
  today: string,
): string | null {
  if (status === 'UNREAD') return null
  return currentReadDate || today
}

export function todayString(): string {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
