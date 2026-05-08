import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ApiConnectionError,
  ApiRequestError,
  ApiServerError,
  configureAuthRefresh,
  request,
  setAccessToken
} from './client'
import { setCurrentLocale } from '../i18n'

describe('api client error handling', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    setAccessToken('')
    configureAuthRefresh(null)
    setCurrentLocale('en')
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
    document.cookie = 'ARTICLESHELF_CSRF=csrf-token'
  })

  afterEach(() => {
    setAccessToken('')
    configureAuthRefresh(null)
    vi.unstubAllGlobals()
    document.cookie = 'ARTICLESHELF_CSRF=; Max-Age=0'
  })

  it('turns API error payloads into structured ApiRequestError instances', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({
      messages: ['This URL is already registered.'],
      existingArticleId: 'article-1'
    }, 409))

    const error = await captureError(() => request('/api/articles', { method: 'POST' }))

    expect(error).toBeInstanceOf(ApiRequestError)
    expect(error).toMatchObject({
      message: 'This URL is already registered.',
      status: 409,
      messages: ['This URL is already registered.'],
      existingArticleId: 'article-1'
    })
  })

  it('uses status-specific fallback messages when an error response has no JSON payload', async () => {
    fetchMock.mockResolvedValueOnce(new Response('', { status: 401 }))

    const error = await captureError(() => request('/api/articles'))

    expect(error).toBeInstanceOf(ApiRequestError)
    expect(error).toMatchObject({
      message: 'Your session has expired. Please log in again.',
      status: 401,
      messages: ['Your session has expired. Please log in again.']
    })
  })

  it('hides server error payload details behind a generic message', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({
      messages: ['internal implementation detail']
    }, 500))

    const error = await captureError(() => request('/api/articles'))

    expect(error).toBeInstanceOf(ApiServerError)
    expect(error).toMatchObject({
      message: 'A server error occurred. Please wait a moment and reload.',
      status: 500
    })
    expect((error as Error).message).not.toContain('internal implementation detail')
  })

  it('turns network failures into localized connection errors', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))

    const error = await captureError(() => request('/api/articles'))

    expect(error).toBeInstanceOf(ApiConnectionError)
    expect(error).toMatchObject({
      message: 'Could not connect to the server. Check that the backend is running.'
    })
  })

  it('refreshes once on 401 and retries with the refreshed access token', async () => {
    setAccessToken('expired-token')
    configureAuthRefresh(() => {
      setAccessToken('fresh-token')
      return Promise.resolve('fresh-token')
    })
    fetchMock
      .mockResolvedValueOnce(new Response('', { status: 401 }))
      .mockResolvedValueOnce(jsonResponse([{ id: 'article-1' }], 200))

    const response = await request<Array<{ id: string }>>('/api/articles')

    expect(response).toEqual([{ id: 'article-1' }])
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer expired-token')
    expect(fetchMock.mock.calls[1][1].headers.Authorization).toBe('Bearer fresh-token')
  })

  it('fails closed when a successful response body is not valid JSON', async () => {
    fetchMock.mockResolvedValueOnce(new Response('', { status: 200 }))

    const error = await captureError(() => request('/api/articles'))

    expect(error).toBeInstanceOf(ApiServerError)
    expect(error).toMatchObject({
      message: 'The server returned an unexpected response. Please reload.',
      status: 200
    })
  })
})

function jsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' }
  })
}

async function captureError(action: () => Promise<unknown>): Promise<unknown> {
  try {
    await action()
  } catch (error: unknown) {
    return error
  }
  throw new Error('Expected action to throw')
}
