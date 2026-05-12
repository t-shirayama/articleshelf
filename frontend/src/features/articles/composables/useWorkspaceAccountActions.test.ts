import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { useWorkspaceAccountActions } from './useWorkspaceAccountActions'

describe('useWorkspaceAccountActions', () => {
  it('resets workspace state after logout and closes the mobile drawer', async () => {
    const authStore = createAuthStore()
    const store = { resetState: vi.fn() }
    const cancelSearch = vi.fn()
    const resetNavigation = vi.fn()
    const accountDialogOpen = ref(false)
    const mobileDrawerOpen = ref(true)

    const actions = useWorkspaceAccountActions({
      authStore: authStore as never,
      store: store as never,
      cancelSearch,
      resetNavigation,
      accountDialogOpen,
      mobileDrawerOpen,
    })

    await actions.logout()

    expect(authStore.logout).toHaveBeenCalled()
    expect(mobileDrawerOpen.value).toBe(false)
    expect(cancelSearch).toHaveBeenCalled()
    expect(store.resetState).toHaveBeenCalled()
    expect(resetNavigation).toHaveBeenCalled()
  })

  it('closes the dialog and resets user scoped state after password change', async () => {
    const authStore = createAuthStore()
    const store = { resetState: vi.fn() }
    const cancelSearch = vi.fn()
    const resetNavigation = vi.fn()
    const accountDialogOpen = ref(true)
    const mobileDrawerOpen = ref(false)

    const actions = useWorkspaceAccountActions({
      authStore: authStore as never,
      store: store as never,
      cancelSearch,
      resetNavigation,
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
      accountDialogOpen,
      mobileDrawerOpen: ref(false),
    })

    await actions.logoutAll()

    expect(actions.accountDialogError.value).toBe('Session expired')
    expect(accountDialogOpen.value).toBe(true)
    expect(store.resetState).not.toHaveBeenCalled()
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
