import type { Article, ArticleInput, ArticleStatus } from '../types'

export interface ArticleDetailForm {
  id: string
  url: string
  title: string
  summary: string
  status: Exclude<ArticleStatus, 'ALL'>
  readDate: string | null
  favorite: boolean
  rating: number
  notes: string
  tags: string[]
}

export interface ArticleCreateForm {
  url: string
  title: string
  summary: string
  readLater: boolean
  readDate: string | null
  favorite: boolean
  rating: number
  notes: string
  tags: string[]
}

export function createEmptyArticleDetailForm(): ArticleDetailForm {
  return {
    id: '',
    url: '',
    title: '',
    summary: '',
    status: 'UNREAD',
    readDate: null,
    favorite: false,
    rating: 0,
    notes: '',
    tags: []
  }
}

export function createEmptyArticleCreateForm(): ArticleCreateForm {
  return {
    url: '',
    title: '',
    summary: '',
    readLater: true,
    readDate: null,
    favorite: false,
    rating: 0,
    notes: '',
    tags: []
  }
}

export function articleToDetailForm(article: Article): ArticleDetailForm {
  return {
    id: article.id,
    url: article.url,
    title: article.title,
    summary: article.summary || '',
    status: article.status || 'UNREAD',
    readDate: article.readDate || null,
    favorite: article.favorite,
    rating: article.rating || 0,
    notes: article.notes || '',
    tags: article.tags?.map((tag) => tag.name) || []
  }
}

export function detailFormToArticleInput(form: ArticleDetailForm): ArticleInput {
  return {
    ...form,
    tags: normalizeTagNames(form.tags),
    readDate: form.readDate || null
  }
}

export function createFormToArticleInput(form: ArticleCreateForm): ArticleInput {
  const readLater = form.readLater
  return {
    url: form.url.trim(),
    title: form.title,
    summary: form.summary,
    status: readLater ? 'UNREAD' : 'READ',
    readDate: readLater ? null : form.readDate || null,
    favorite: form.favorite,
    rating: form.rating,
    notes: form.notes,
    tags: normalizeTagNames(form.tags)
  }
}

export function favoriteToggleInput(article: Article): ArticleInput {
  return {
    ...articleToDetailForm(article),
    tags: article.tags.map((tag) => tag.name),
    readDate: article.readDate || null,
    favorite: !article.favorite
  }
}

export function normalizeTagNames(tags: string[]): string[] {
  return [...new Set(tags.map((tag) => tag.trim()).filter(Boolean))]
}
