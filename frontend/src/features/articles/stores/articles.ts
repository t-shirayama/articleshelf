import { defineStore } from 'pinia'
import { errorMessage } from '../../../shared/errors'
import { translate } from '../../../shared/i18n'
import { articlesApi } from '../api/articlesApi'
import { createDefaultArticleFilters, filterArticles, sortArticles } from '../domain/articleFilters'
import { replaceArticle, toArticleInput } from '../domain/articleMappers'
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
    filters: createDefaultArticleFilters(),
    loading: false,
    error: ''
  }),
  getters: {
    filteredArticles: (state): Article[] => filterArticles(state.articles, state.filters),
    sortedArticles(): Article[] {
      return sortArticles(this.filteredArticles, this.filters.sort)
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
        const articles = await articlesApi.findArticles(createDefaultArticleFilters())
        this.articles = articles
        this.allArticles = articles
        if (this.selectedArticle) {
          const selectedId = this.selectedArticle.id
          this.selectedArticle = this.articles.find((article) => article.id === selectedId) || null
        }
      } catch (error: unknown) {
        this.error = errorMessage(error, translate('articles.fetchError'))
      } finally {
        this.loading = false
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
      await this.fetchArticles()
    },
    async mergeTag(sourceId: string, targetTagId: string): Promise<void> {
      await articlesApi.mergeTag(sourceId, targetTagId)
      await this.fetchTags()
      await this.fetchArticles()
    },
    async deleteTag(id: string): Promise<void> {
      await articlesApi.deleteTag(id)
      await this.fetchTags()
      await this.fetchArticles()
    },
    async selectArticle(article: Article): Promise<void> {
      this.selectedArticle = await articlesApi.findArticle(article.id)
    },
    async selectArticleById(articleId: string): Promise<void> {
      this.selectedArticle = await articlesApi.findArticle(articleId)
    },
    async createArticle(article: ArticleInput): Promise<void> {
      const created = await articlesApi.createArticle(article)
      await this.fetchTags()
      await this.fetchArticles()
      this.selectedArticle = created
    },
    async updateArticle(article: ArticleInput): Promise<void> {
      if (!article.id) throw new Error(translate('articles.missingUpdateTarget'))
      const updated = await articlesApi.updateArticle(article.id, article)
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
      this.applyArticleUpdate(optimisticArticle)

      try {
        const updated = await articlesApi.updateArticle(article.id, toArticleInput(optimisticArticle))
        this.applyArticleUpdate(updated)
      } catch (error: unknown) {
        this.articles = previousArticles
        this.allArticles = previousAllArticles
        this.selectedArticle = previousSelectedArticle
        this.error = errorMessage(error, translate('articles.favoriteError'))
      }
    },
    async updateArticleStatus(article: Article, status: Exclude<ArticleStatus, 'ALL'>, readDate: string | null): Promise<Article | null> {
      const previousArticles = this.articles
      const previousAllArticles = this.allArticles
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
        this.allArticles = previousAllArticles
        this.selectedArticle = previousSelectedArticle
        this.error = errorMessage(error, translate('articles.statusError'))
        return null
      }
    },
    applyArticleUpdate(article: Article): void {
      this.allArticles = replaceArticle(this.allArticles, article)
      this.articles = replaceArticle(this.articles, article)

      if (this.selectedArticle?.id === article.id) {
        this.selectedArticle = article
      }
    },
    async deleteArticle(articleId: string): Promise<void> {
      await articlesApi.deleteArticle(articleId)
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
    },
    resetState(): void {
      this.articles = []
      this.allArticles = []
      this.tags = []
      this.selectedArticle = null
      this.filters = createDefaultArticleFilters()
      this.loading = false
      this.error = ''
    }
  }
})
