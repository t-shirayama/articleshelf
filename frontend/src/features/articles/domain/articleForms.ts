import type { Article, ArticleInput, ArticleStatus } from '../types'

export interface ArticleDetailForm {
  id: string
  version: number
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
    version: 0,
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
    version: article.version,
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

export function hasArticleDetailFormChanges(form: ArticleDetailForm, article: Article): boolean {
  return !areArticleInputsEqual(
    detailFormToArticleInput(form),
    detailFormToArticleInput(articleToDetailForm(article))
  )
}

export function createFormToArticleInput(form: ArticleCreateForm): ArticleInput {
  const readLater = form.readLater
  const input: ArticleInput = {
    url: form.url.trim(),
    status: readLater ? 'UNREAD' : 'READ',
    readDate: readLater ? null : form.readDate || null,
    favorite: form.favorite,
    rating: form.rating,
    tags: normalizeTagNames(form.tags)
  }
  const title = form.title.trim()
  const summary = form.summary.trim()
  const notes = form.notes.trim()
  if (title) input.title = title
  if (summary) input.summary = summary
  if (notes) input.notes = notes
  return input
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

function areArticleInputsEqual(current: ArticleInput, original: ArticleInput): boolean {
  return (
    current.id === original.id &&
    current.version === original.version &&
    current.url === original.url &&
    current.title === original.title &&
    current.summary === original.summary &&
    current.status === original.status &&
    current.readDate === original.readDate &&
    current.favorite === original.favorite &&
    current.rating === original.rating &&
    current.notes === original.notes &&
    areStringArraysEqual(current.tags, original.tags)
  )
}

function areStringArraysEqual(current: string[] = [], original: string[] = []): boolean {
  return current.length === original.length && current.every((value, index) => value === original[index])
}
