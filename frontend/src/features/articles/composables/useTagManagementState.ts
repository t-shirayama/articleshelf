import { computed, nextTick, onMounted, ref, watch, type Ref } from "vue";
import { getCurrentLocale } from "../../../shared/i18n";
import type { Tag } from "../types";

export type TagSort = "COUNT_DESC" | "COUNT_ASC" | "NAME_ASC";

export function useTagManagementState(tags: Ref<Tag[]>, t: (key: string, params?: Record<string, unknown>) => string) {
  const addDialogOpen = ref(false);
  const addDraft = ref("");
  const renameTagId = ref("");
  const renameDraft = ref("");
  const mergeSource = ref<Tag | null>(null);
  const mergeTargetId = ref("");
  const deleteCandidate = ref<Tag | null>(null);
  const searchQuery = ref<string | null>("");
  const sortMode = ref<TagSort>("COUNT_DESC");
  const sortMeasureEl = ref<HTMLElement | null>(null);
  const sortWidth = ref(252);

  const sortOptions = computed(() => [
    { title: t("tags.sortCountDesc"), value: "COUNT_DESC" },
    { title: t("tags.sortCountAsc"), value: "COUNT_ASC" },
    { title: t("tags.sortNameAsc"), value: "NAME_ASC" },
  ]);

  const longestSortTitle = computed(() =>
    sortOptions.value.reduce(
      (longest, option) => (option.title.length > longest.length ? option.title : longest),
      "",
    ),
  );

  const sortWidthStyle = computed(() => ({
    "--tag-management-sort-width": `${sortWidth.value}px`,
  }));

  const filteredTags = computed(() => {
    const keyword = (searchQuery.value || "").trim().toLocaleLowerCase(currentTextLocale());
    if (!keyword) return tags.value;
    return tags.value.filter((tag) =>
      tag.name.toLocaleLowerCase(currentTextLocale()).includes(keyword),
    );
  });

  const sortedTags = computed(() => {
    const sortableTags = [...filteredTags.value];
    return sortableTags.sort((left, right) => {
      const leftCount = left.articleCount || 0;
      const rightCount = right.articleCount || 0;
      if (sortMode.value === "COUNT_ASC") {
        return leftCount - rightCount || left.name.localeCompare(right.name, "ja");
      }
      if (sortMode.value === "NAME_ASC") {
        return left.name.localeCompare(right.name, "ja");
      }
      return rightCount - leftCount || left.name.localeCompare(right.name, "ja");
    });
  });

  const mergeOptions = computed(() =>
    tags.value
      .filter((tag) => tag.id && tag.id !== mergeSource.value?.id)
      .map((tag) => ({
        title: `${tag.name} (${tag.articleCount || 0})`,
        value: tag.id || "",
      })),
  );

  async function updateSortWidth(): Promise<void> {
    await nextTick();
    const measuredWidth = sortMeasureEl.value?.scrollWidth || 0;
    sortWidth.value = Math.ceil(measuredWidth + 78);
  }

  function openAddDialog(): void {
    addDraft.value = "";
    addDialogOpen.value = true;
  }

  function closeAddDialog(saving: boolean): void {
    if (saving) return;
    addDialogOpen.value = false;
    addDraft.value = "";
  }

  function startRename(tag: Tag): void {
    renameTagId.value = tag.id || "";
    renameDraft.value = tag.name;
  }

  function cancelRename(): void {
    renameTagId.value = "";
    renameDraft.value = "";
  }

  function openMerge(tag: Tag): void {
    mergeSource.value = tag;
    mergeTargetId.value = "";
  }

  function closeMerge(): void {
    mergeSource.value = null;
    mergeTargetId.value = "";
  }

  function requestDelete(tag: Tag): void {
    if ((tag.articleCount || 0) > 0) return;
    deleteCandidate.value = tag;
  }

  function deleteTooltip(tag: Tag): string {
    return (tag.articleCount || 0) > 0 ? t("tags.deleteDisabledTooltip") : t("tags.deleteTooltip");
  }

  function currentTextLocale(): string {
    return getCurrentLocale() === "ja" ? "ja" : "en";
  }

  watch(longestSortTitle, () => {
    void updateSortWidth();
  });

  onMounted(() => {
    void updateSortWidth();
  });

  return {
    addDialogOpen,
    addDraft,
    renameTagId,
    renameDraft,
    mergeSource,
    mergeTargetId,
    deleteCandidate,
    searchQuery,
    sortMode,
    sortMeasureEl,
    sortOptions,
    longestSortTitle,
    sortWidthStyle,
    sortedTags,
    mergeOptions,
    openAddDialog,
    closeAddDialog,
    startRename,
    cancelRename,
    openMerge,
    closeMerge,
    requestDelete,
    deleteTooltip,
  };
}
