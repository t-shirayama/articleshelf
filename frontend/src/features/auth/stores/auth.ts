import { defineStore } from 'pinia'
import { errorMessage } from '../../../shared/errors'
import { translate } from '../../../shared/i18n'
import { configureAuthRefresh, setAccessToken } from '../../../shared/api/client'
import { authApi } from '../api/authApi'
import { ProactiveRefreshTimer } from '../services/proactiveRefreshTimer'
import type { AuthCredentials, ChangePasswordInput, DeleteAccountInput, RegisterInput, User } from '../types'

const proactiveRefreshTimer = new ProactiveRefreshTimer()

interface AuthState {
  user: User | null
  accessToken: string
  authReady: boolean
  loading: boolean
  error: string
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    accessToken: '',
    authReady: false,
    loading: false,
    error: ''
  }),
  getters: {
    isAuthenticated: (state): boolean => Boolean(state.user && state.accessToken)
  },
  actions: {
    async initialize(): Promise<void> {
      configureAuthRefresh(() => this.refreshSession())
      try {
        await this.refreshSession()
      } catch {
        this.clearSession()
      } finally {
        this.authReady = true
      }
    },
    async register(input: RegisterInput): Promise<void> {
      this.loading = true
      this.error = ''
      try {
        this.applyAuthResponse(await authApi.register(input))
      } catch (error: unknown) {
        this.error = errorMessage(error, translate('auth.errors.registerFailed'))
        throw error
      } finally {
        this.loading = false
      }
    },
    async login(input: AuthCredentials): Promise<void> {
      this.loading = true
      this.error = ''
      try {
        this.applyAuthResponse(await authApi.login(input))
      } catch (error: unknown) {
        this.error = errorMessage(error, translate('auth.errors.loginFailed'))
        throw error
      } finally {
        this.loading = false
      }
    },
    async refreshSession(): Promise<string | null> {
      try {
        const response = await authApi.refresh()
        this.applyAuthResponse(response)
        return response.accessToken
      } catch {
        this.clearSession()
        return null
      }
    },
    async logout(): Promise<void> {
      try {
        await authApi.logout()
      } finally {
        this.clearSession()
      }
    },
    async logoutAll(): Promise<void> {
      this.loading = true
      this.error = ''
      try {
        await authApi.logoutAll()
      } catch (error: unknown) {
        this.error = errorMessage(error, translate('auth.account.logoutAllFailed'))
        throw error
      } finally {
        this.loading = false
        this.clearSession()
      }
    },
    async changePassword(input: ChangePasswordInput): Promise<void> {
      this.loading = true
      this.error = ''
      try {
        await authApi.changePassword(input)
      } catch (error: unknown) {
        this.error = errorMessage(error, translate('auth.account.passwordChangeFailed'))
        throw error
      } finally {
        this.loading = false
      }
      this.clearSession()
    },
    async deleteAccount(input: DeleteAccountInput): Promise<void> {
      this.loading = true
      this.error = ''
      try {
        await authApi.deleteAccount(input)
      } catch (error: unknown) {
        this.error = errorMessage(error, translate('auth.account.deleteFailed'))
        throw error
      } finally {
        this.loading = false
      }
      this.clearSession()
    },
    clearSession(): void {
      this.user = null
      this.accessToken = ''
      setAccessToken('')
      proactiveRefreshTimer.clear()
    },
    applyAuthResponse(response: { user: User, accessToken: string }): void {
      this.user = response.user
      this.accessToken = response.accessToken
      setAccessToken(response.accessToken)
      this.scheduleProactiveRefresh(response.accessToken)
    },
    scheduleProactiveRefresh(token: string): void {
      proactiveRefreshTimer.schedule(token, () => this.refreshSession())
    }
  }
})
