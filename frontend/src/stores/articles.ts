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
      tag: '',
      search: '',
      favorite: false,
      sort: 'CREATED_DESC'
    },
    loading: false,
    error: ''
  }),
  getters: {
    sortedArticles: (state): Article[] => [...state.articles].sort((left, right) => compareArticles(left, right, state.filters.sort)),
    counts: (state) => ({
      all: state.allArticles.length,
      unread: state.allArticles.filter((article) => article.status === 'UNREAD').length,
      read: state.allArticles.filter((article) => article.status === 'READ').length,
      favorite: state.allArticles.filter((article) => article.favorite).length
    })
  },
  actions: {
    async fetchArticles(): Promise<void> {
      this.loading = true
      this.error = ''
      try {
        const [articles, allArticles] = await Promise.all([
          api.findArticles(this.filters),
          api.findArticles(allArticleFilters())
        ])
        this.articles = articles
        this.allArticles = allArticles
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
      this.articles = this.filters.favorite && !article.favorite
        ? this.articles.filter((item) => item.id !== article.id)
        : replaceArticle(this.articles, article)

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
      return this.fetchArticles()
    },
    setTag(tag: string): Promise<void> {
      this.filters.tag = tag
      return this.fetchArticles()
    },
    setAllArticles(): Promise<void> {
      this.filters.status = 'ALL'
      this.filters.tag = ''
      this.filters.favorite = false
      return this.fetchArticles()
    },
    setSearch(search: string): Promise<void> {
      this.filters.search = search
      return this.fetchArticles()
    },
    setFavoriteOnly(): Promise<void> {
      this.filters.status = 'ALL'
      this.filters.tag = ''
      this.filters.favorite = true
      return this.fetchArticles()
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
    tag: '',
    search: '',
    favorite: false,
    sort: 'CREATED_DESC'
  }
}
