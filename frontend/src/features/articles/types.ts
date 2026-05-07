export type ArticleStatus = 'ALL' | 'UNREAD' | 'READ'
export type ArticleSort = 'CREATED_DESC' | 'CREATED_ASC' | 'UPDATED_DESC' | 'READ_DATE_DESC' | 'TITLE_ASC' | 'RATING_DESC'

export interface ArticleDateRange {
  from: string
  to: string
}

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
  thumbnailUrl?: string | null
  status: Exclude<ArticleStatus, 'ALL'>
  readDate?: string | null
  favorite: boolean
  rating: number
  notes?: string | null
  tags: Tag[]
  createdAt?: string
  updatedAt?: string
}

export interface ArticleFilters {
  status: ArticleStatus
  tags: string[]
  ratings: number[]
  createdRange: ArticleDateRange
  readRange: ArticleDateRange
  search: string
  favorite: boolean
  sort: ArticleSort
}

export interface ArticleInput {
  id?: string
  url: string
  title: string
  summary?: string | null
  status: Exclude<ArticleStatus, 'ALL'>
  readDate?: string | null
  favorite: boolean
  rating: number
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
