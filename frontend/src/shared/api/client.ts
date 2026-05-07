const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const CSRF_COOKIE = 'READSTACK_CSRF'

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
  skipAuthorization?: boolean
  skipAuthRetry?: boolean
}

let accessToken = ''
let refreshAccessToken: (() => Promise<string | null>) | null = null
let refreshPromise: Promise<string | null> | null = null

export function setAccessToken(token: string): void {
  accessToken = token
}

export function configureAuthRefresh(callback: () => Promise<string | null>): void {
  refreshAccessToken = callback
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return performRequest<T>(path, options, false)
}

async function performRequest<T>(path: string, options: RequestOptions, retried: boolean): Promise<T> {
  const { skipAuthorization, skipAuthRetry, headers, ...fetchOptions } = options
  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...authorizationHeader(skipAuthorization),
        ...csrfHeader(fetchOptions.method),
        ...headers
      },
      ...fetchOptions
    })
  } catch (error: unknown) {
    if (error instanceof TypeError) {
      throw new Error('サーバーに接続できませんでした。バックエンドが起動しているか確認してください。')
    }
    throw error
  }

  if (response.status === 401 && !retried && !skipAuthRetry && refreshAccessToken) {
    const refreshedToken = await refreshOnce()
    if (refreshedToken) {
      return performRequest<T>(path, options, true)
    }
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

async function refreshOnce(): Promise<string | null> {
  if (!refreshAccessToken) return null
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

function authorizationHeader(skipAuthorization?: boolean): Record<string, string> {
  if (skipAuthorization || !accessToken) return {}
  return { Authorization: `Bearer ${accessToken}` }
}

function csrfHeader(method?: string): Record<string, string> {
  const normalizedMethod = (method || 'GET').toUpperCase()
  if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(normalizedMethod)) return {}
  const token = readCookie(CSRF_COOKIE)
  return token ? { 'X-CSRF-Token': token } : {}
}

function readCookie(name: string): string {
  return document.cookie
    .split(';')
    .map((value) => value.trim())
    .find((value) => value.startsWith(`${name}=`))
    ?.slice(name.length + 1) || ''
}
