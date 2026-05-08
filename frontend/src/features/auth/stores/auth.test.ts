import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import { authApi } from '../api/authApi'
import { setAccessToken } from '../../../shared/api/client'

vi.mock('../api/authApi', () => ({
  authApi: {
    register: vi.fn(),
    login: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
    logoutAll: vi.fn(),
    me: vi.fn(),
    changePassword: vi.fn(),
    deleteAccount: vi.fn()
  }
}))

vi.mock('../../../shared/api/client', () => ({
  configureAuthRefresh: vi.fn(),
  setAccessToken: vi.fn()
}))

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('stores username based auth responses', async () => {
    const store = useAuthStore()
    vi.mocked(authApi.login).mockResolvedValueOnce({
      user: user(),
      accessToken: token()
    })

    await store.login({ username: 'reader', password: 'password123' })

    expect(store.user?.username).toBe('reader')
    expect(store.isAuthenticated).toBe(true)
    expect(setAccessToken).toHaveBeenCalledWith(token())
  })

  it('clears auth state after password change', async () => {
    const store = useAuthStore()
    store.applyAuthResponse({ user: user(), accessToken: token() })
    vi.mocked(authApi.changePassword).mockResolvedValueOnce(null)

    await store.changePassword({
      currentPassword: 'password123',
      newPassword: 'new-password123'
    })

    expect(authApi.changePassword).toHaveBeenCalledWith({
      currentPassword: 'password123',
      newPassword: 'new-password123'
    })
    expect(store.user).toBeNull()
    expect(store.accessToken).toBe('')
    expect(setAccessToken).toHaveBeenLastCalledWith('')
  })

  it('clears auth state after logout-all', async () => {
    const store = useAuthStore()
    store.applyAuthResponse({ user: user(), accessToken: token() })
    vi.mocked(authApi.logoutAll).mockResolvedValueOnce(null)

    await store.logoutAll()

    expect(authApi.logoutAll).toHaveBeenCalled()
    expect(store.user).toBeNull()
    expect(store.accessToken).toBe('')
  })

  it('clears auth state after account deletion', async () => {
    const store = useAuthStore()
    store.applyAuthResponse({ user: user(), accessToken: token() })
    vi.mocked(authApi.deleteAccount).mockResolvedValueOnce(null)

    await store.deleteAccount({ currentPassword: 'password123' })

    expect(authApi.deleteAccount).toHaveBeenCalledWith({ currentPassword: 'password123' })
    expect(store.user).toBeNull()
    expect(store.accessToken).toBe('')
  })
})

function user() {
  return {
    id: 'user-1',
    username: 'reader',
    displayName: 'Reader',
    roles: ['USER']
  }
}

function token(): string {
  return [
    'header',
    window.btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })),
    'signature'
  ].join('.')
}
