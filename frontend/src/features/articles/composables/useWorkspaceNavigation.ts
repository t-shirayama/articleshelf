import { computed, ref } from "vue";

export type WorkspaceViewMode = "list" | "calendar" | "detail" | "tags";

export function useWorkspaceNavigation(onViewChange?: () => void) {
  const viewMode = ref<WorkspaceViewMode>("list");
  const detailHasUnsavedChanges = ref(false);
  const unsavedChangesDialogOpen = ref(false);
  const pendingNavigation = ref<(() => void) | null>(null);

  const isListMode = computed(() => viewMode.value === "list");
  const isCalendarActive = computed(() => viewMode.value === "calendar");
  const isTagsActive = computed(() => viewMode.value === "tags");

  function requestNavigation(action: () => void): void {
    if (viewMode.value === "detail" && detailHasUnsavedChanges.value) {
      pendingNavigation.value = action;
      unsavedChangesDialogOpen.value = true;
      return;
    }

    action();
  }

  function navigateToList(): void {
    if (viewMode.value !== "list") onViewChange?.();
    viewMode.value = "list";
  }

  function navigateToCalendar(): void {
    if (viewMode.value !== "calendar") onViewChange?.();
    viewMode.value = "calendar";
  }

  function navigateToTags(): void {
    if (viewMode.value !== "tags") onViewChange?.();
    viewMode.value = "tags";
  }

  function cancelPendingNavigation(): void {
    pendingNavigation.value = null;
    unsavedChangesDialogOpen.value = false;
  }

  function confirmPendingNavigation(): void {
    const action = pendingNavigation.value;
    pendingNavigation.value = null;
    unsavedChangesDialogOpen.value = false;
    detailHasUnsavedChanges.value = false;
    action?.();
  }

  function resetNavigation(): void {
    detailHasUnsavedChanges.value = false;
    pendingNavigation.value = null;
    unsavedChangesDialogOpen.value = false;
    viewMode.value = "list";
  }

  return {
    viewMode,
    detailHasUnsavedChanges,
    unsavedChangesDialogOpen,
    isListMode,
    isCalendarActive,
    isTagsActive,
    requestNavigation,
    navigateToList,
    navigateToCalendar,
    navigateToTags,
    cancelPendingNavigation,
    confirmPendingNavigation,
    resetNavigation,
  };
}
