import { ref } from "vue";
import type { Ref } from "vue";
import type { useArticlesStore } from "../stores/articles";
import type { useAuthStore } from "../../auth/stores/auth";

type AccountDialogInput = { currentPassword: string; newPassword: string };
type DeleteAccountInput = { currentPassword: string };

interface UseWorkspaceAccountActionsOptions {
  authStore: ReturnType<typeof useAuthStore>;
  store: ReturnType<typeof useArticlesStore>;
  cancelSearch: () => void;
  resetNavigation: () => void;
  navigateToLogin: () => Promise<unknown> | void;
  accountDialogOpen: Ref<boolean>;
  mobileDrawerOpen: Ref<boolean>;
}

export function useWorkspaceAccountActions({
  authStore,
  store,
  cancelSearch,
  resetNavigation,
  navigateToLogin,
  accountDialogOpen,
  mobileDrawerOpen,
}: UseWorkspaceAccountActionsOptions) {
  const accountDialogError = ref("");

  function resetUserScopedState(): void {
    cancelSearch();
    store.resetState();
    resetNavigation();
  }

  async function logout(): Promise<void> {
    mobileDrawerOpen.value = false;
    await authStore.logout();
    resetUserScopedState();
    await navigateToLogin();
  }

  async function changePassword(input: AccountDialogInput): Promise<void> {
    accountDialogError.value = "";
    try {
      await authStore.changePassword(input);
      resetUserScopedState();
      accountDialogOpen.value = false;
      await navigateToLogin();
    } catch {
      accountDialogError.value = authStore.error;
    }
  }

  async function logoutAll(): Promise<void> {
    accountDialogError.value = "";
    try {
      await authStore.logoutAll();
      resetUserScopedState();
      accountDialogOpen.value = false;
      await navigateToLogin();
    } catch {
      accountDialogError.value = authStore.error;
    }
  }

  async function deleteAccount(input: DeleteAccountInput): Promise<void> {
    accountDialogError.value = "";
    try {
      await authStore.deleteAccount(input);
      resetUserScopedState();
      accountDialogOpen.value = false;
      await navigateToLogin();
    } catch {
      accountDialogError.value = authStore.error;
    }
  }

  function openAccountSettings(): void {
    mobileDrawerOpen.value = false;
    accountDialogOpen.value = true;
  }

  return {
    accountDialogError,
    logout,
    changePassword,
    logoutAll,
    deleteAccount,
    openAccountSettings,
  };
}
