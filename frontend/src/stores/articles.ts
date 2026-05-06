import { defineStore } from 'pinia'
import { api } from '../services/api'
import type { Article, ArticleFilters, ArticleInput, ArticleSort, ArticleStatus, Tag } from '../types'

interface ArticlesState {
  articles: Article[]
  allArticles: Article[]
  tags: Tag[]
  selectedArticle: Article | null
  filters: ArticleFilters
  loading: boolean
  error: string
}

export const useArticlesStore = defineStore('articles', {
  state: (): ArticlesState => ({
    articles: [],
    allArticles: [],
    tags: [],
    selectedArticle: null,
    filters: {
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
    },
    loading: false,
    error: ''
  }),
  getters: {
    filteredArticles: (state): Article[] => state.articles.filter((article) => matchesFilters(article, state.filters)),
    sortedArticles(): Article[] {
      return [...this.filteredArticles].sort((left, right) => compareArticles(left, right, this.filters.sort))
    },
    counts: (state) => ({
      all: state.articles.length,
      unread: state.articles.filter((article) => article.status === 'UNREAD').length,
      read: state.articles.filter((article) => article.status === 'READ').length,
      favorite: state.articles.filter((article) => article.favorite).length
    })
  },
  actions: {
    async fetchArticles(): Promise<void> {
      this.loading = true
      this.error = ''
      try {
        const articles = await api.findArticles(allArticleFilters())
        this.articles = articles
        this.allArticles = articles
        if (this.selectedArticle) {
          const selectedId = this.selectedArticle.id
          this.selectedArticle = this.articles.find((article) => article.id === selectedId) || null
        }
      } catch (error: unknown) {
        this.error = error instanceof Error ? error.message : '記事の取得に失敗しました'
      } finally {
        this.loading = false
      }
    },
    async fetchTags(): Promise<void> {
      this.tags = await api.findTags()
    },
    async selectArticle(article: Article): Promise<void> {
      this.selectedArticle = await api.findArticle(article.id)
    },
    async selectArticleById(articleId: string): Promise<void> {
      this.selectedArticle = await api.findArticle(articleId)
    },
    async createArticle(article: ArticleInput): Promise<void> {
      const created = await api.createArticle(article)
      await this.fetchTags()
      await this.fetchArticles()
      this.selectedArticle = created
    },
    async updateArticle(article: ArticleInput): Promise<void> {
      if (!article.id) throw new Error('更新対象の記事IDがありません')
      const updated = await api.updateArticle(article.id, article)
      await this.fetchTags()
      await this.fetchArticles()
      this.selectedArticle = updated
    },
    async toggleFavorite(article: Article): Promise<void> {
      const previousArticles = this.articles
      const previousAllArticles = this.allArticles
      const previousSelectedArticle = this.selectedArticle
      const optimisticArticle = { ...article, favorite: !article.favorite }
      this.error = ''
      this.applyFavoriteUpdate(optimisticArticle)

      try {
        const updated = await api.updateArticle(article.id, toArticleInput(optimisticArticle))
        this.applyFavoriteUpdate(updated)
      } catch (error: unknown) {
        this.articles = previousArticles
        this.allArticles = previousAllArticles
        this.selectedArticle = previousSelectedArticle
        this.error = error instanceof Error ? error.message : 'お気に入りの更新に失敗しました'
      }
    },
    applyFavoriteUpdate(article: Article): void {
      this.allArticles = replaceArticle(this.allArticles, article)
      this.articles = replaceArticle(this.articles, article)

      if (this.selectedArticle?.id === article.id) {
        this.selectedArticle = article
      }
    },
    async deleteArticle(articleId: string): Promise<void> {
      await api.deleteArticle(articleId)
      this.selectedArticle = null
      await this.fetchTags()
      await this.fetchArticles()
    },
    setStatus(status: ArticleStatus): Promise<void> {
      this.filters.status = status
      this.filters.favorite = false
      return Promise.resolve()
    },
    setTags(tags: string[]): void {
      this.filters.tags = [...tags]
    },
    setRatings(ratings: number[]): void {
      this.filters.ratings = [...ratings].sort((left, right) => left - right)
    },
    setCreatedRange(range: { from: string, to: string }): void {
      this.filters.createdRange = { ...range }
    },
    setReadRange(range: { from: string, to: string }): void {
      this.filters.readRange = { ...range }
    },
    clearAdvancedFilters(): void {
      this.filters.tags = []
      this.filters.ratings = []
      this.filters.createdRange = { from: '', to: '' }
      this.filters.readRange = { from: '', to: '' }
    },
    setAllArticles(): Promise<void> {
      this.filters.status = 'ALL'
      this.clearAdvancedFilters()
      this.filters.favorite = false
      return Promise.resolve()
    },
    setSearch(search: string): void {
      this.filters.search = search
    },
    setFavoriteOnly(): Promise<void> {
      this.filters.status = 'ALL'
      this.clearAdvancedFilters()
      this.filters.favorite = true
      return Promise.resolve()
    },
    setSort(sort: ArticleSort): void {
      this.filters.sort = sort
    }
  }
})

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

function replaceArticle(articles: Article[], updated: Article): Article[] {
  return articles.map((article) => article.id === updated.id ? updated : article)
}

function toArticleInput(article: Article): ArticleInput {
  return {
    id: article.id,
    url: article.url,
    title: article.title,
    summary: article.summary || '',
    status: article.status,
    readDate: article.readDate || null,
    favorite: article.favorite,
    rating: article.rating,
    notes: article.notes || '',
    tags: article.tags?.map((tag) => tag.name).filter(Boolean) || []
  }
}

function allArticleFilters(): ArticleFilters {
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

  if (!matchesSearch(article, filters.search)) {
    return false
  }

  return true
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
