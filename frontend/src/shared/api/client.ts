import { getCurrentLocale, translate } from "../i18n"

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const CSRF_COOKIE = 'READSTACK_CSRF'

interface ApiErrorPayload {
  messages?: string[]
  existingArticleId?: string | null
}

export class ApiRequestError extends Error {
  readonly status: number
  readonly messages: string[]
  existingArticleId?: string

  constructor(message: string, existingArticleId?: string | null, status = 0, messages: string[] = [message]) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.messages = messages
    this.existingArticleId = existingArticleId || undefined
  }
}

export class ApiConnectionError extends Error {
  constructor(message = translate('errors.connection')) {
    super(message)
    this.name = 'ApiConnectionError'
  }
}

export class ApiServerError extends Error {
  readonly status: number

  constructor(message = translate('errors.server'), status = 0) {
    super(message)
    this.name = 'ApiServerError'
    this.status = status
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

export function configureAuthRefresh(callback: (() => Promise<string | null>) | null): void {
  refreshAccessToken = callback
  refreshPromise = null
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
        'Accept-Language': getCurrentLocale(),
        ...authorizationHeader(skipAuthorization),
        ...csrfHeader(fetchOptions.method),
        ...headers
      },
      ...fetchOptions
    })
  } catch (error: unknown) {
    if (error instanceof TypeError) {
      throw new ApiConnectionError()
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

  const payload = await readJson(response)
  if (!response.ok) {
    if (response.status >= 500) {
      throw new ApiServerError(translate('errors.server'), response.status)
    }
    const errorPayload = toApiErrorPayload(payload)
    const messages = errorPayload?.messages?.filter((message) => message.trim()) || []
    const message = messages.length ? messages.join(', ') : fallbackErrorMessage(response.status)
    throw new ApiRequestError(message, errorPayload?.existingArticleId, response.status, messages.length ? messages : [message])
  }
  if (payload === null) {
    throw new ApiServerError(translate('errors.invalidResponse'), response.status)
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

async function readJson(response: Response): Promise<unknown | null> {
  return response.json().catch(() => null)
}

function toApiErrorPayload(payload: unknown): ApiErrorPayload | null {
  if (!payload || typeof payload !== 'object') return null
  const candidate = payload as ApiErrorPayload
  return Array.isArray(candidate.messages) || 'existingArticleId' in candidate ? candidate : null
}

function fallbackErrorMessage(status: number): string {
  if (status === 401) return translate('errors.unauthorized')
  if (status === 403) return translate('errors.forbidden')
  if (status === 404) return translate('errors.notFound')
  return translate('errors.api')
}
