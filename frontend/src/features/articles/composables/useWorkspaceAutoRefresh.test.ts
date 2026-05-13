import { beforeEach, describe, expect, it, vi } from "vitest";
import { useWorkspaceAutoRefresh } from "./useWorkspaceAutoRefresh";

describe("useWorkspaceAutoRefresh", () => {
  let currentTime: number;
  let refresh: ReturnType<typeof vi.fn<() => Promise<void>>>;
  let hasUnsavedChanges: ReturnType<typeof vi.fn<() => boolean>>;

  beforeEach(() => {
    currentTime = 10_000;
    refresh = vi.fn().mockResolvedValue(undefined);
    hasUnsavedChanges = vi.fn().mockReturnValue(false);
  });

  it("refreshes articles when the window receives focus after the throttle interval", async () => {
    const autoRefresh = createAutoRefresh();
    autoRefresh.markLoaded();
    autoRefresh.mount();
    currentTime += 5_000;

    window.dispatchEvent(new Event("focus"));
    await Promise.resolve();

    expect(refresh).toHaveBeenCalledTimes(1);
    autoRefresh.dispose();
  });

  it("refreshes only when the document becomes visible", async () => {
    const autoRefresh = createAutoRefresh();
    autoRefresh.markLoaded();
    autoRefresh.mount();
    currentTime += 5_000;

    stubVisibilityState("hidden");
    document.dispatchEvent(new Event("visibilitychange"));
    await Promise.resolve();
    expect(refresh).not.toHaveBeenCalled();

    stubVisibilityState("visible");
    document.dispatchEvent(new Event("visibilitychange"));
    await Promise.resolve();
    expect(refresh).toHaveBeenCalledTimes(1);
    autoRefresh.dispose();
  });

  it("throttles repeated foreground events within five seconds", async () => {
    const autoRefresh = createAutoRefresh();
    autoRefresh.markLoaded();
    autoRefresh.mount();
    currentTime += 5_000;

    window.dispatchEvent(new Event("focus"));
    await Promise.resolve();
    currentTime += 4_999;
    window.dispatchEvent(new Event("focus"));
    await Promise.resolve();

    expect(refresh).toHaveBeenCalledTimes(1);
    autoRefresh.dispose();
  });

  it("skips refresh while the detail view has unsaved changes", async () => {
    hasUnsavedChanges.mockReturnValue(true);
    const autoRefresh = createAutoRefresh();
    autoRefresh.markLoaded();
    autoRefresh.mount();
    currentTime += 5_000;

    window.dispatchEvent(new Event("focus"));
    await Promise.resolve();

    expect(refresh).not.toHaveBeenCalled();
    autoRefresh.dispose();
  });

  it("does not start another refresh while one is already running", async () => {
    let resolveRefresh = (): void => {};
    refresh.mockReturnValue(new Promise<void>((resolve) => {
      resolveRefresh = resolve;
    }));
    const autoRefresh = createAutoRefresh();
    autoRefresh.markLoaded();
    autoRefresh.mount();
    currentTime += 5_000;

    window.dispatchEvent(new Event("focus"));
    await Promise.resolve();
    currentTime += 5_000;
    window.dispatchEvent(new Event("focus"));
    await Promise.resolve();

    expect(refresh).toHaveBeenCalledTimes(1);
    resolveRefresh();
    await Promise.resolve();
    autoRefresh.dispose();
  });

  function createAutoRefresh() {
    return useWorkspaceAutoRefresh({
      refresh,
      hasUnsavedChanges,
      now: () => currentTime,
    });
  }
});

function stubVisibilityState(value: DocumentVisibilityState): void {
  vi.spyOn(document, "visibilityState", "get").mockReturnValue(value);
}
