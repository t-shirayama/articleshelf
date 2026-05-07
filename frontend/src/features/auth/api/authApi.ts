import { request } from '../../../shared/api/client'
import type { AuthCredentials, AuthResponse, RegisterInput, User } from '../types'

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
  me(): Promise<User> {
    return request<User>('/api/users/me')
  }
}
