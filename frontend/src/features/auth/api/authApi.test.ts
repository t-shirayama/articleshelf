import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '../../../shared/api/client'
import { authApi } from './authApi'

vi.mock('../../../shared/api/client', () => ({
  request: vi.fn()
}))

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(request).mockResolvedValue(null)
  })

  it('marks public auth endpoints as not requiring authorization or retry', async () => {
    await authApi.register({ username: 'reader', displayName: 'Reader', password: 'password123' })
    await authApi.login({ username: 'reader', password: 'password123' })
    await authApi.refresh()

    expect(vi.mocked(request).mock.calls.slice(0, 3)).toEqual([
      ['/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({ username: 'reader', displayName: 'Reader', password: 'password123' }),
        skipAuthorization: true,
        skipAuthRetry: true
      }],
      ['/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username: 'reader', password: 'password123' }),
        skipAuthorization: true,
        skipAuthRetry: true
      }],
      ['/api/auth/refresh', {
        method: 'POST',
        skipAuthorization: true,
        skipAuthRetry: true
      }]
    ])
  })

  it('uses retry-safe options for session ending and account mutation endpoints', async () => {
    await authApi.logout()
    await authApi.logoutAll()
    await authApi.me()
    await authApi.changePassword({ currentPassword: 'old-password', newPassword: 'new-password' })
    await authApi.deleteAccount({ currentPassword: 'password123' })
    await authApi.adminResetPassword('admin@example.com', { newPassword: 'reset-password' })

    expect(vi.mocked(request).mock.calls).toEqual([
      ['/api/auth/logout', { method: 'POST', skipAuthRetry: true }],
      ['/api/auth/logout-all', { method: 'POST', skipAuthRetry: true }],
      ['/api/users/me'],
      ['/api/users/me/password', {
        method: 'PATCH',
        body: JSON.stringify({ currentPassword: 'old-password', newPassword: 'new-password' }),
        skipAuthRetry: true
      }],
      ['/api/users/me', {
        method: 'DELETE',
        body: JSON.stringify({ currentPassword: 'password123' }),
        skipAuthRetry: true
      }],
      ['/api/admin/users/admin%40example.com/password', {
        method: 'POST',
        body: JSON.stringify({ newPassword: 'reset-password' })
      }]
    ])
  })
})
