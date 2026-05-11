import { request } from '../../../shared/api/client'
import type { Article, ArticleFilters, ArticleInput, ArticlePreview, Tag } from '../types'

export const articlesApi = {
  findArticles(filters: ArticleFilters): Promise<Article[]> {
    const params = new URLSearchParams()
    if (filters.status && filters.status !== 'ALL') params.set('status', filters.status)
    if (filters.search) params.set('search', filters.search)
    if (filters.favorite) params.set('favorite', 'true')
    const query = params.toString()
    return request<Article[]>(`/api/articles${query ? `?${query}` : ''}`)
  },
  findArticle(id: string): Promise<Article> {
    return request<Article>(`/api/articles/${id}`)
  },
  createArticle(article: ArticleInput): Promise<Article> {
    return request<Article>('/api/articles', {
      method: 'POST',
      body: JSON.stringify(article)
    })
  },
  previewArticle(url: string): Promise<ArticlePreview> {
    return request<ArticlePreview>('/api/articles/preview', {
      method: 'POST',
      body: JSON.stringify({ url })
    })
  },
  updateArticle(id: string, article: ArticleInput): Promise<Article> {
    return request<Article>(`/api/articles/${id}`, {
      method: 'PUT',
      body: JSON.stringify(article)
    })
  },
  deleteArticle(id: string): Promise<null> {
    return request<null>(`/api/articles/${id}`, {
      method: 'DELETE'
    })
  },
  findTags(): Promise<Tag[]> {
    return request<Tag[]>('/api/tags')
  },
  createTag(name: string): Promise<Tag> {
    return request<Tag>('/api/tags', {
      method: 'POST',
      body: JSON.stringify({ name })
    })
  },
  renameTag(id: string, name: string): Promise<Tag> {
    return request<Tag>(`/api/tags/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ name })
    })
  },
  mergeTag(sourceId: string, targetTagId: string): Promise<null> {
    return request<null>(`/api/tags/${sourceId}/merge`, {
      method: 'POST',
      body: JSON.stringify({ targetTagId })
    })
  },
  deleteTag(id: string): Promise<null> {
    return request<null>(`/api/tags/${id}`, {
      method: 'DELETE'
    })
  }
}
