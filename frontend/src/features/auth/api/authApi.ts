import { request } from '../../../shared/api/client'
import type {
  AdminResetPasswordInput,
  AuthCredentials,
  AuthResponse,
  ChangePasswordInput,
  DeleteAccountInput,
  RegisterInput,
  User
} from '../types'

export const authApi = {
  register(input: RegisterInput): Promise<AuthResponse> {
    return request<AuthResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(input),
      skipAuthorization: true,
      skipAuthRetry: true
    })
  },
  login(input: AuthCredentials): Promise<AuthResponse> {
    return request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(input),
      skipAuthorization: true,
      skipAuthRetry: true
    })
  },
  refresh(): Promise<AuthResponse> {
    return request<AuthResponse>('/api/auth/refresh', {
      method: 'POST',
      skipAuthorization: true,
      skipAuthRetry: true
    })
  },
  logout(): Promise<null> {
    return request<null>('/api/auth/logout', {
      method: 'POST',
      skipAuthRetry: true
    })
  },
  logoutAll(): Promise<null> {
    return request<null>('/api/auth/logout-all', {
      method: 'POST',
      skipAuthRetry: true
    })
  },
  me(): Promise<User> {
    return request<User>('/api/users/me')
  },
  changePassword(input: ChangePasswordInput): Promise<null> {
    return request<null>('/api/users/me/password', {
      method: 'PATCH',
      body: JSON.stringify(input),
      skipAuthRetry: true
    })
  },
  deleteAccount(input: DeleteAccountInput): Promise<null> {
    return request<null>('/api/users/me', {
      method: 'DELETE',
      body: JSON.stringify(input),
      skipAuthRetry: true
    })
  },
  adminResetPassword(username: string, input: AdminResetPasswordInput): Promise<null> {
    return request<null>(`/api/admin/users/${encodeURIComponent(username)}/password`, {
      method: 'POST',
      body: JSON.stringify(input)
    })
  }
}
