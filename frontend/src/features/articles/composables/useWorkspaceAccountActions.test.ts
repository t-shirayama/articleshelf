import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { useWorkspaceAccountActions } from './useWorkspaceAccountActions'

describe('useWorkspaceAccountActions', () => {
  it('resets workspace state after logout and closes the mobile drawer', async () => {
    const authStore = createAuthStore()
    const store = { resetState: vi.fn() }
    const cancelSearch = vi.fn()
    const resetNavigation = vi.fn()
    const navigateToLogin = vi.fn()
    const accountDialogOpen = ref(false)
    const mobileDrawerOpen = ref(true)

    const actions = useWorkspaceAccountActions({
      authStore: authStore as never,
      store: store as never,
      cancelSearch,
      resetNavigation,
      navigateToLogin,
      accountDialogOpen,
      mobileDrawerOpen,
    })

    await actions.logout()

    expect(authStore.logout).toHaveBeenCalled()
    expect(mobileDrawerOpen.value).toBe(false)
    expect(cancelSearch).toHaveBeenCalled()
    expect(store.resetState).toHaveBeenCalled()
    expect(resetNavigation).toHaveBeenCalled()
    expect(navigateToLogin).toHaveBeenCalled()
  })

  it('closes the dialog and resets user scoped state after password change', async () => {
    const authStore = createAuthStore()
    const store = { resetState: vi.fn() }
    const cancelSearch = vi.fn()
    const resetNavigation = vi.fn()
    const navigateToLogin = vi.fn()
    const accountDialogOpen = ref(true)
    const mobileDrawerOpen = ref(false)

    const actions = useWorkspaceAccountActions({
      authStore: authStore as never,
      store: store as never,
      cancelSearch,
      resetNavigation,
      navigateToLogin,
      accountDialogOpen,
      mobileDrawerOpen,
    })

    await actions.changePassword({
      currentPassword: 'old-password',
      newPassword: 'new-password',
    })

    expect(authStore.changePassword).toHaveBeenCalledWith({
      currentPassword: 'old-password',
      newPassword: 'new-password',
    })
    expect(accountDialogOpen.value).toBe(false)
    expect(actions.accountDialogError.value).toBe('')
    expect(cancelSearch).toHaveBeenCalled()
    expect(store.resetState).toHaveBeenCalled()
    expect(resetNavigation).toHaveBeenCalled()
    expect(navigateToLogin).toHaveBeenCalled()
  })

  it('surfaces auth errors when an account operation fails', async () => {
    const authStore = createAuthStore()
    authStore.logoutAll.mockRejectedValueOnce(new Error('logout failed'))
    authStore.error = 'Session expired'
    const store = { resetState: vi.fn() }
    const accountDialogOpen = ref(true)

    const actions = useWorkspaceAccountActions({
      authStore: authStore as never,
      store: store as never,
      cancelSearch: vi.fn(),
      resetNavigation: vi.fn(),
      navigateToLogin: vi.fn(),
      accountDialogOpen,
      mobileDrawerOpen: ref(false),
    })

    await actions.logoutAll()

    expect(actions.accountDialogError.value).toBe('Session expired')
    expect(accountDialogOpen.value).toBe(true)
    expect(store.resetState).not.toHaveBeenCalled()
  })

  it('deletes the account and resets the scoped workspace state', async () => {
    const authStore = createAuthStore()
    const store = { resetState: vi.fn() }
    const cancelSearch = vi.fn()
    const resetNavigation = vi.fn()
    const navigateToLogin = vi.fn()
    const accountDialogOpen = ref(true)

    const actions = useWorkspaceAccountActions({
      authStore: authStore as never,
      store: store as never,
      cancelSearch,
      resetNavigation,
      navigateToLogin,
      accountDialogOpen,
      mobileDrawerOpen: ref(false),
    })

    await actions.deleteAccount({ currentPassword: 'current-password' })

    expect(authStore.deleteAccount).toHaveBeenCalledWith({
      currentPassword: 'current-password',
    })
    expect(actions.accountDialogError.value).toBe('')
    expect(accountDialogOpen.value).toBe(false)
    expect(cancelSearch).toHaveBeenCalled()
    expect(store.resetState).toHaveBeenCalled()
    expect(resetNavigation).toHaveBeenCalled()
    expect(navigateToLogin).toHaveBeenCalled()
  })

  it('keeps the account dialog open when account deletion fails', async () => {
    const authStore = createAuthStore()
    authStore.deleteAccount.mockRejectedValueOnce(new Error('delete failed'))
    authStore.error = 'Current password is incorrect'
    const store = { resetState: vi.fn() }
    const cancelSearch = vi.fn()
    const resetNavigation = vi.fn()
    const navigateToLogin = vi.fn()
    const accountDialogOpen = ref(true)

    const actions = useWorkspaceAccountActions({
      authStore: authStore as never,
      store: store as never,
      cancelSearch,
      resetNavigation,
      navigateToLogin,
      accountDialogOpen,
      mobileDrawerOpen: ref(false),
    })

    await actions.deleteAccount({ currentPassword: 'wrong-password' })

    expect(actions.accountDialogError.value).toBe('Current password is incorrect')
    expect(accountDialogOpen.value).toBe(true)
    expect(cancelSearch).not.toHaveBeenCalled()
    expect(store.resetState).not.toHaveBeenCalled()
    expect(resetNavigation).not.toHaveBeenCalled()
    expect(navigateToLogin).not.toHaveBeenCalled()
  })

  it('opens account settings and closes the mobile drawer', () => {
    const accountDialogOpen = ref(false)
    const mobileDrawerOpen = ref(true)
    const actions = useWorkspaceAccountActions({
      authStore: createAuthStore() as never,
      store: { resetState: vi.fn() } as never,
      cancelSearch: vi.fn(),
      resetNavigation: vi.fn(),
      navigateToLogin: vi.fn(),
      accountDialogOpen,
      mobileDrawerOpen,
    })

    actions.openAccountSettings()

    expect(mobileDrawerOpen.value).toBe(false)
    expect(accountDialogOpen.value).toBe(true)
  })
})

function createAuthStore() {
  return {
    error: '',
    logout: vi.fn().mockResolvedValue(undefined),
    changePassword: vi.fn().mockResolvedValue(undefined),
    logoutAll: vi.fn().mockResolvedValue(undefined),
    deleteAccount: vi.fn().mockResolvedValue(undefined),
  }
}
