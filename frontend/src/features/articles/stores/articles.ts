import { defineStore } from 'pinia'
import { errorMessage } from '../../../shared/errors'
import { translate } from '../../../shared/i18n'
import { articlesApi } from '../api/articlesApi'
import { createDefaultArticleFilters } from '../domain/articleFilters'
import { replaceArticle, toArticleInput } from '../domain/articleMappers'
import type { Article, ArticleFilters, ArticleInput, ArticleSort, ArticleStatus, Tag } from '../types'

const DEFAULT_LIST_PAGE_SIZE = 20

interface ArticlesState {
  articles: Article[]
  articleSnapshot: Article[]
  tags: Tag[]
  selectedArticle: Article | null
  filters: ArticleFilters
  currentPage: number
  hasNextPage: boolean
  loading: boolean
  error: string
}

export const useArticlesStore = defineStore('articles', {
  state: (): ArticlesState => ({
    articles: [],
    articleSnapshot: [],
    tags: [],
    selectedArticle: null,
    filters: createDefaultArticleFilters(),
    currentPage: 0,
    hasNextPage: false,
    loading: false,
    error: ''
  }),
  getters: {
    counts: (state) => {
      const source = state.articleSnapshot.length > 0 ? state.articleSnapshot : state.articles
      return {
        all: source.length,
        unread: source.filter((article) => article.status === 'UNREAD').length,
        read: source.filter((article) => article.status === 'READ').length,
        favorite: source.filter((article) => article.favorite).length
      }
    },
    hasPreviousPage: (state) => state.currentPage > 0
  },
  actions: {
    async fetchArticles(): Promise<void> {
      this.loading = true
      this.error = ''
      try {
        const probeSize = DEFAULT_LIST_PAGE_SIZE + 1
        const articles = await articlesApi.findArticles(this.filters, {
          page: this.currentPage,
          size: probeSize
        })
        this.hasNextPage = articles.length > DEFAULT_LIST_PAGE_SIZE
        this.articles = this.hasNextPage ? articles.slice(0, DEFAULT_LIST_PAGE_SIZE) : articles
        if (this.selectedArticle) {
          const selectedId = this.selectedArticle.id
          this.selectedArticle = this.articles.find((article) => article.id === selectedId) || this.selectedArticle
        }
      } catch (error: unknown) {
        this.error = errorMessage(error, translate('articles.fetchError'))
      } finally {
        this.loading = false
      }
    },
    async fetchArticleSnapshot(): Promise<void> {
      try {
        this.articleSnapshot = await articlesApi.findArticles(createDefaultArticleFilters())
        if (this.selectedArticle) {
          const selectedId = this.selectedArticle.id
          this.selectedArticle = this.articleSnapshot.find((article) => article.id === selectedId) || this.selectedArticle
        }
      } catch (error: unknown) {
        this.error = errorMessage(error, translate('articles.fetchError'))
      }
    },
    async fetchTags(): Promise<void> {
      try {
        this.tags = await articlesApi.findTags()
      } catch (error: unknown) {
        this.error = errorMessage(error, translate('tags.fetchError'))
      }
    },
    async createTag(name: string): Promise<void> {
      await articlesApi.createTag(name)
      await this.fetchTags()
    },
    async renameTag(id: string, name: string): Promise<void> {
      await articlesApi.renameTag(id, name)
      await this.fetchTags()
      await Promise.all([this.fetchArticles(), this.fetchArticleSnapshot()])
    },
    async mergeTag(sourceId: string, targetTagId: string): Promise<void> {
      await articlesApi.mergeTag(sourceId, targetTagId)
      await this.fetchTags()
      await Promise.all([this.fetchArticles(), this.fetchArticleSnapshot()])
    },
    async deleteTag(id: string): Promise<void> {
      await articlesApi.deleteTag(id)
      await this.fetchTags()
      await Promise.all([this.fetchArticles(), this.fetchArticleSnapshot()])
    },
    async selectArticle(article: Article): Promise<void> {
      this.selectedArticle = await articlesApi.findArticle(article.id)
    },
    async selectArticleById(articleId: string): Promise<void> {
      this.selectedArticle = await articlesApi.findArticle(articleId)
    },
    async createArticle(article: ArticleInput): Promise<Article> {
      const created = await articlesApi.createArticle(article)
      await this.fetchTags()
      await Promise.all([this.fetchArticles(), this.fetchArticleSnapshot()])
      this.selectedArticle = created
      return created
    },
    async updateArticle(article: ArticleInput): Promise<void> {
      if (!article.id) throw new Error(translate('articles.missingUpdateTarget'))
      const updated = await articlesApi.updateArticle(article.id, article)
      await this.fetchTags()
      await Promise.all([this.fetchArticles(), this.fetchArticleSnapshot()])
      this.selectedArticle = updated
    },
    async toggleFavorite(article: Article): Promise<void> {
      const previousArticles = this.articles
      const previousSnapshot = this.articleSnapshot
      const previousSelectedArticle = this.selectedArticle
      const optimisticArticle = { ...article, favorite: !article.favorite }
      this.error = ''
      this.applyArticleUpdate(optimisticArticle)

      try {
        const updated = await articlesApi.updateArticle(article.id, toArticleInput(optimisticArticle))
        this.applyArticleUpdate(updated)
      } catch (error: unknown) {
        this.articles = previousArticles
        this.articleSnapshot = previousSnapshot
        this.selectedArticle = previousSelectedArticle
        this.error = errorMessage(error, translate('articles.favoriteError'))
      }
    },
    async updateArticleStatus(article: Article, status: Exclude<ArticleStatus, 'ALL'>, readDate: string | null): Promise<Article | null> {
      const previousArticles = this.articles
      const previousSnapshot = this.articleSnapshot
      const previousSelectedArticle = this.selectedArticle
      const optimisticArticle = { ...article, status, readDate }
      this.error = ''
      this.applyArticleUpdate(optimisticArticle)

      try {
        const updated = await articlesApi.updateArticle(article.id, toArticleInput(optimisticArticle))
        this.applyArticleUpdate(updated)
        return updated
      } catch (error: unknown) {
        this.articles = previousArticles
        this.articleSnapshot = previousSnapshot
        this.selectedArticle = previousSelectedArticle
        this.error = errorMessage(error, translate('articles.statusError'))
        return null
      }
    },
    applyArticleUpdate(article: Article): void {
      this.articles = replaceArticle(this.articles, article)
      this.articleSnapshot = replaceArticle(this.articleSnapshot, article)

      if (this.selectedArticle?.id === article.id) {
        this.selectedArticle = article
      }
    },
    async deleteArticle(articleId: string): Promise<void> {
      await articlesApi.deleteArticle(articleId)
      this.selectedArticle = null
      await this.fetchTags()
      await Promise.all([this.fetchArticles(), this.fetchArticleSnapshot()])
    },
    setStatus(status: ArticleStatus): void {
      this.filters.status = status
      this.filters.favorite = false
      this.resetListPage()
    },
    setTags(tags: string[]): void {
      this.filters.tags = [...tags]
      this.resetListPage()
    },
    setRatings(ratings: number[]): void {
      this.filters.ratings = [...ratings].sort((left, right) => left - right)
      this.resetListPage()
    },
    setCreatedRange(range: { from: string, to: string }): void {
      this.filters.createdRange = { ...range }
      this.resetListPage()
    },
    setReadRange(range: { from: string, to: string }): void {
      this.filters.readRange = { ...range }
      this.resetListPage()
    },
    clearAdvancedFilters(): void {
      this.filters.tags = []
      this.filters.ratings = []
      this.filters.createdRange = { from: '', to: '' }
      this.filters.readRange = { from: '', to: '' }
    },
    setAllArticles(): void {
      this.filters.status = 'ALL'
      this.clearAdvancedFilters()
      this.filters.favorite = false
      this.resetListPage()
    },
    setSearch(search: string): void {
      this.filters.search = search
      this.resetListPage()
    },
    setFavoriteOnly(): void {
      this.filters.status = 'ALL'
      this.clearAdvancedFilters()
      this.filters.favorite = true
      this.resetListPage()
    },
    setSort(sort: ArticleSort): void {
      this.filters.sort = sort
      this.resetListPage()
    },
    async goToNextPage(): Promise<void> {
      if (!this.hasNextPage) return
      this.currentPage += 1
      await this.fetchArticles()
    },
    async goToPreviousPage(): Promise<void> {
      if (this.currentPage === 0) return
      this.currentPage -= 1
      await this.fetchArticles()
    },
    resetListPage(): void {
      this.currentPage = 0
      this.hasNextPage = false
    },
    resetState(): void {
      this.articles = []
      this.articleSnapshot = []
      this.tags = []
      this.selectedArticle = null
      this.filters = createDefaultArticleFilters()
      this.currentPage = 0
      this.hasNextPage = false
      this.loading = false
      this.error = ''
    }
  }
})
