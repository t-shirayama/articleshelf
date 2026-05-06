export type ArticleStatus = 'ALL' | 'UNREAD' | 'READ'

export interface Tag {
  id?: string
  name: string
  createdAt?: string
  updatedAt?: string
}

export interface Article {
  id: string
  url: string
  title: string
  summary?: string | null
  status: Exclude<ArticleStatus, 'ALL'>
  readDate?: string | null
  favorite: boolean
  notes?: string | null
  tags: Tag[]
  createdAt?: string
  updatedAt?: string
}

export interface ArticleFilters {
  status: ArticleStatus
  tag: string
  search: string
  favorite: boolean
}

export interface ArticleInput {
  id?: string
  url: string
  title: string
  summary?: string | null
  status: Exclude<ArticleStatus, 'ALL'>
  readDate?: string | null
  favorite: boolean
  notes?: string | null
  tags: string[]
}

export interface MotivationCardData {
  id: number
  title: string
  note: string
  illustration: string
  background: string
  accent: string
  ink: string
}
