import type { Article, ArticleFilters, ArticleSort } from '../types'

export function createDefaultArticleFilters(): ArticleFilters {
  return {
    status: 'ALL',
    tags: [],
    ratings: [],
    createdRange: {
      from: '',
      to: ''
    },
    readRange: {
      from: '',
      to: ''
    },
    search: '',
    favorite: false,
    sort: 'CREATED_DESC'
  }
}

export function filterArticles(articles: Article[], filters: ArticleFilters): Article[] {
  return articles.filter((article) => matchesFilters(article, filters))
}

export function sortArticles(articles: Article[], sort: ArticleSort): Article[] {
  return [...articles].sort((left, right) => compareArticles(left, right, sort))
}

function compareArticles(left: Article, right: Article, sort: ArticleSort): number {
  switch (sort) {
    case 'CREATED_ASC':
      return compareDate(left.createdAt, right.createdAt)
    case 'UPDATED_DESC':
      return compareDate(right.updatedAt, left.updatedAt)
    case 'READ_DATE_DESC':
      return compareDate(right.readDate, left.readDate) || compareDate(right.updatedAt, left.updatedAt)
    case 'TITLE_ASC':
      return left.title.localeCompare(right.title, 'ja')
    case 'RATING_DESC':
      return right.rating - left.rating || compareDate(right.updatedAt, left.updatedAt)
    case 'CREATED_DESC':
    default:
      return compareDate(right.createdAt, left.createdAt)
  }
}

function compareDate(left?: string | null, right?: string | null): number {
  const leftTime = left ? Date.parse(left) : Number.NEGATIVE_INFINITY
  const rightTime = right ? Date.parse(right) : Number.NEGATIVE_INFINITY
  return leftTime - rightTime
}

function matchesFilters(article: Article, filters: ArticleFilters): boolean {
  if (filters.status !== 'ALL' && article.status !== filters.status) {
    return false
  }

  if (filters.favorite && !article.favorite) {
    return false
  }

  if (filters.tags.length > 0) {
    const articleTagNames = article.tags.map((tag) => tag.name)
    const matchesTag = filters.tags.some((tag) => articleTagNames.includes(tag))
    if (!matchesTag) {
      return false
    }
  }

  if (filters.ratings.length > 0 && !filters.ratings.includes(article.rating)) {
    return false
  }

  if (!matchesDateRange(article.createdAt, filters.createdRange.from, filters.createdRange.to)) {
    return false
  }

  if (!matchesDateRange(article.readDate, filters.readRange.from, filters.readRange.to)) {
    return false
  }

  return matchesSearch(article, filters.search)
}

function matchesSearch(article: Article, search: string): boolean {
  const keyword = search.trim().toLocaleLowerCase('ja')
  if (!keyword) return true

  const haystacks = [
    article.title,
    article.url,
    article.notes || '',
    article.tags.map((tag) => tag.name).join(' ')
  ]

  return haystacks.some((value) => value.toLocaleLowerCase('ja').includes(keyword))
}

function matchesDateRange(value: string | null | undefined, from: string, to: string): boolean {
  if (!from && !to) return true
  if (!value) return false

  const target = value.slice(0, 10)
  if (from && target < from) return false
  if (to && target > to) return false
  return true
}
