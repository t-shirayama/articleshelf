import { pinia } from "../../../app/providers/pinia";
import { useAuthStore } from "../stores/auth";

let authInitialization: Promise<void> | null = null;

export function ensureAuthReady(): Promise<void> {
  const authStore = useAuthStore(pinia);
  if (authStore.authReady) {
    return Promise.resolve();
  }

  authInitialization ??= authStore.initialize().finally(() => {
    authInitialization = null;
  });

  return authInitialization;
}
