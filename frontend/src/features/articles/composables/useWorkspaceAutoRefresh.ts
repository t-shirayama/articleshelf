interface WorkspaceAutoRefreshOptions {
  refresh: () => Promise<void>;
  hasUnsavedChanges: () => boolean;
  now?: () => number;
  minIntervalMs?: number;
}

const DEFAULT_MIN_INTERVAL_MS = 5_000;

export function useWorkspaceAutoRefresh(options: WorkspaceAutoRefreshOptions) {
  const now = options.now ?? Date.now;
  const minIntervalMs = options.minIntervalMs ?? DEFAULT_MIN_INTERVAL_MS;
  let lastRefreshAt = 0;
  let refreshPromise: Promise<void> | null = null;

  function markLoaded(): void {
    lastRefreshAt = now();
  }

  async function refreshIfStale(): Promise<void> {
    if (options.hasUnsavedChanges()) return;

    const currentTime = now();
    if (refreshPromise || currentTime - lastRefreshAt < minIntervalMs) return;

    lastRefreshAt = currentTime;
    refreshPromise = options.refresh().finally(() => {
      refreshPromise = null;
    });

    await refreshPromise;
  }

  function handleFocus(): void {
    void refreshIfStale();
  }

  function handleVisibilityChange(): void {
    if (document.visibilityState === "visible") {
      void refreshIfStale();
    }
  }

  function mount(): void {
    window.addEventListener("focus", handleFocus);
    document.addEventListener("visibilitychange", handleVisibilityChange);
  }

  function dispose(): void {
    window.removeEventListener("focus", handleFocus);
    document.removeEventListener("visibilitychange", handleVisibilityChange);
  }

  return {
    markLoaded,
    refreshIfStale,
    mount,
    dispose,
  };
}
