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

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
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
