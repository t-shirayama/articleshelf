import { defineStore } from 'pinia'
import { api } from '../services/api'

export const useArticlesStore = defineStore('articles', {
  state: () => ({
    articles: [],
    tags: [],
    selectedArticle: null,
    filters: {
      status: 'ALL',
      tag: '',
      search: '',
      favorite: false
    },
    loading: false,
    error: ''
  }),
  getters: {
    counts: (state) => ({
      all: state.articles.length,
      unread: state.articles.filter((article) => article.status === 'UNREAD').length,
      read: state.articles.filter((article) => article.status === 'READ').length,
      favorite: state.articles.filter((article) => article.favorite).length
    })
  },
  actions: {
    async fetchArticles() {
      this.loading = true
      this.error = ''
      try {
        this.articles = await api.findArticles(this.filters)
        if (this.selectedArticle) {
          this.selectedArticle = this.articles.find((article) => article.id === this.selectedArticle.id) || null
        }
      } catch (error) {
        this.error = error.message
      } finally {
        this.loading = false
      }
    },
    async fetchTags() {
      this.tags = await api.findTags()
    },
    async selectArticle(article) {
      this.selectedArticle = await api.findArticle(article.id)
    },
    async createArticle(article) {
      const created = await api.createArticle(article)
      await this.fetchTags()
      await this.fetchArticles()
      this.selectedArticle = created
    },
    async updateArticle(article) {
      const updated = await api.updateArticle(article.id, article)
      await this.fetchTags()
      await this.fetchArticles()
      this.selectedArticle = updated
    },
    async toggleFavorite(article) {
      const tagNames = article.tags?.map((tag) => tag.name || tag).filter(Boolean) || []
      await this.updateArticle({
        ...article,
        favorite: !article.favorite,
        tags: tagNames
      })
    },
    async deleteArticle(articleId) {
      await api.deleteArticle(articleId)
      this.selectedArticle = null
      await this.fetchTags()
      await this.fetchArticles()
    },
    setStatus(status) {
      this.filters.status = status
      this.filters.favorite = false
      return this.fetchArticles()
    },
    setTag(tag) {
      this.filters.tag = tag
      return this.fetchArticles()
    },
    setSearch(search) {
      this.filters.search = search
      return this.fetchArticles()
    },
    setFavoriteOnly() {
      this.filters.status = 'ALL'
      this.filters.tag = ''
      this.filters.favorite = true
      return this.fetchArticles()
    }
  }
})
