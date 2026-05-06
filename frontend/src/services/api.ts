import type { Article, ArticleFilters, ArticleInput, Tag } from '../types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

interface ApiErrorPayload {
  messages?: string[]
  existingArticleId?: string | null
}

export class ApiRequestError extends Error {
  existingArticleId?: string

  constructor(message: string, existingArticleId?: string | null) {
    super(message)
    this.name = 'ApiRequestError'
    this.existingArticleId = existingArticleId || undefined
  }
}

type RequestOptions = Omit<RequestInit, 'headers'> & {
  headers?: Record<string, string>
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers
      },
      ...options
    })
  } catch (error: unknown) {
    if (error instanceof TypeError) {
      throw new Error('サーバーに接続できませんでした。バックエンドが起動しているか確認してください。')
    }
    throw error
  }

  if (response.status === 204) {
    return null as T
  }

  const payload = await response.json().catch(() => null) as T | ApiErrorPayload | null
  if (!response.ok) {
    if (response.status >= 500) {
      throw new Error('サーバー側でエラーが発生しました。少し待ってから再読み込みしてください。')
    }
    const errorPayload = payload as ApiErrorPayload | null
    const message = errorPayload?.messages?.join(', ') || 'API request failed'
    throw new ApiRequestError(message, errorPayload?.existingArticleId)
  }
  return payload as T
}

export const api = {
  findArticles(filters: ArticleFilters): Promise<Article[]> {
    const params = new URLSearchParams()
    if (filters.status && filters.status !== 'ALL') params.set('status', filters.status)
    if (filters.tag) params.set('tag', filters.tag)
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
  }
}
