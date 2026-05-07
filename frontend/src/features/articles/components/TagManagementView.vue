<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { Check, GitMerge, Pencil, Plus, Search, Trash2, X } from "lucide-vue-next";
import { getCurrentLocale } from "../../../shared/i18n";
import type { Tag } from "../types";

type TagSort = "COUNT_DESC" | "COUNT_ASC" | "NAME_ASC";

const props = defineProps<{
  tags: Tag[];
  error: string;
  loading: boolean;
  saving: boolean;
}>();

const emit = defineEmits<{
  addTag: [name: string];
  renameTag: [id: string, name: string];
  mergeTag: [sourceId: string, targetId: string];
  deleteTag: [id: string];
  openTagArticles: [tagName: string];
  retry: [];
}>();

const { t } = useI18n();
const addDialogOpen = ref(false);
const addDraft = ref("");
const renameTagId = ref("");
const renameDraft = ref("");
const mergeSource = ref<Tag | null>(null);
const mergeTargetId = ref("");
const deleteCandidate = ref<Tag | null>(null);
const searchQuery = ref<string | null>("");
const sortMode = ref<TagSort>("COUNT_DESC");

const sortOptions = computed(() => [
  { title: t("tags.sortCountDesc"), value: "COUNT_DESC" },
  { title: t("tags.sortCountAsc"), value: "COUNT_ASC" },
  { title: t("tags.sortNameAsc"), value: "NAME_ASC" },
]);

const sortWidthStyle = computed(() => {
  const longestLength = sortOptions.value.reduce(
    (maxLength, option) => Math.max(maxLength, Array.from(option.title).length),
    0,
  );
  const textWidth = currentTextLocale() === "ja" ? `${longestLength}em` : `${longestLength}ch`;
  return { "--tag-management-sort-width": `calc(${textWidth} + 104px)` };
});

const filteredTags = computed(() => {
  const keyword = (searchQuery.value || "").trim().toLocaleLowerCase(currentTextLocale());
  if (!keyword) return props.tags;
  return props.tags.filter((tag) =>
    tag.name.toLocaleLowerCase(currentTextLocale()).includes(keyword),
  );
});

const sortedTags = computed(() => {
  const tags = [...filteredTags.value];
  return tags.sort((left, right) => {
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
  props.tags
    .filter((tag) => tag.id && tag.id !== mergeSource.value?.id)
    .map((tag) => ({
      title: `${tag.name} (${tag.articleCount || 0})`,
      value: tag.id || "",
    })),
);

function openAddDialog(): void {
  addDraft.value = "";
  addDialogOpen.value = true;
}

function closeAddDialog(): void {
  if (props.saving) return;
  addDialogOpen.value = false;
  addDraft.value = "";
}

function confirmAdd(): void {
  const name = addDraft.value.trim();
  if (!name) return;
  emit("addTag", name);
  addDraft.value = "";
  addDialogOpen.value = false;
}

function startRename(tag: Tag): void {
  renameTagId.value = tag.id || "";
  renameDraft.value = tag.name;
}

function cancelRename(): void {
  renameTagId.value = "";
  renameDraft.value = "";
}

function confirmRename(): void {
  const name = renameDraft.value.trim();
  if (!renameTagId.value || !name) return;
  emit("renameTag", renameTagId.value, name);
  cancelRename();
}

function openMerge(tag: Tag): void {
  mergeSource.value = tag;
  mergeTargetId.value = "";
}

function closeMerge(): void {
  mergeSource.value = null;
  mergeTargetId.value = "";
}

function confirmMerge(): void {
  if (!mergeSource.value?.id || !mergeTargetId.value) return;
  emit("mergeTag", mergeSource.value.id, mergeTargetId.value);
  closeMerge();
}

function requestDelete(tag: Tag): void {
  if ((tag.articleCount || 0) > 0) return;
  deleteCandidate.value = tag;
}

function confirmDelete(): void {
  if (!deleteCandidate.value?.id) return;
  emit("deleteTag", deleteCandidate.value.id);
  deleteCandidate.value = null;
}

function deleteTooltip(tag: Tag): string {
  return (tag.articleCount || 0) > 0 ? t("tags.deleteDisabledTooltip") : t("tags.deleteTooltip");
}

function currentTextLocale(): string {
  return getCurrentLocale() === "ja" ? "ja" : "en";
}
</script>

<template>
  <section class="tag-management-view">
    <header class="page-header tag-management-header">
      <div>
        <h1>
          {{ t("tags.title") }}
          <span class="tag-management-title-count">{{ t("tags.count", { count: tags.length }) }}</span>
        </h1>
      </div>
      <div class="tag-management-toolbar">
        <VTextField
          v-model="searchQuery"
          class="search-input tag-management-search"
          type="text"
          clearable
          hide-details
          :placeholder="t('tags.searchPlaceholder')"
        >
          <template #prepend-inner>
            <Search :size="18" />
          </template>
        </VTextField>
        <VSelect
          v-model="sortMode"
          class="readstack-select sort-select tag-management-sort"
          :style="sortWidthStyle"
          :items="sortOptions"
          item-title="title"
          item-value="value"
          hide-details
          density="comfortable"
          variant="outlined"
          :label="t('tags.sortLabel')"
        />
        <VBtn
          class="action-button action-button-primary tag-management-add-button"
          color="primary"
          variant="flat"
          @click="openAddDialog"
        >
          <template #prepend>
            <Plus :size="18" />
          </template>
          {{ t("tags.add") }}
        </VBtn>
      </div>
    </header>

    <div
      v-if="error"
      class="error-banner"
      role="alert"
      aria-live="assertive"
    >
      <strong>{{ t("tags.operationErrorTitle") }}</strong>
      <span>{{ error }}</span>
      <div class="error-banner-actions">
        <VBtn
          class="action-button action-button-secondary error-banner-action"
          variant="outlined"
          size="small"
          @click="emit('retry')"
        >
          {{ t("common.retry") }}
        </VBtn>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <VProgressCircular indeterminate color="primary" size="42" />
    </div>

    <div v-else-if="tags.length === 0" class="empty-state">
      <h2>{{ t("tags.emptyTitle") }}</h2>
      <p>{{ t("tags.emptyBody") }}</p>
      <VBtn
        class="action-button action-button-primary"
        color="primary"
        variant="flat"
        @click="openAddDialog"
      >
        <template #prepend>
          <Plus :size="18" />
        </template>
        {{ t("tags.addFirst") }}
      </VBtn>
    </div>

    <div v-else class="tag-management-list" aria-live="polite">
      <div class="tag-management-row tag-management-row-head">
        <span>{{ t("tags.name") }}</span>
        <span>{{ t("tags.articles") }}</span>
        <span>{{ t("tags.actions") }}</span>
      </div>

      <div
        v-for="tag in sortedTags"
        :key="tag.id || tag.name"
        class="tag-management-row"
      >
        <div class="tag-management-name-cell">
          <template v-if="renameTagId === tag.id">
            <VTextField
              v-model="renameDraft"
              class="tag-management-rename-input"
              density="compact"
              hide-details
              autofocus
              @keyup.enter="confirmRename"
              @keyup.esc="cancelRename"
            />
            <VBtn
              icon
              size="small"
              variant="text"
              color="primary"
              :loading="saving"
              :disabled="saving || !renameDraft.trim()"
              :aria-label="t('tags.saveName')"
              @click="confirmRename"
            >
              <Check :size="18" />
            </VBtn>
            <VBtn
              icon
              size="small"
              variant="text"
              :aria-label="t('tags.cancelRename')"
              @click="cancelRename"
            >
              <X :size="18" />
            </VBtn>
          </template>
          <template v-else>
            <button
              class="tag-management-tag-link"
              type="button"
              @click="emit('openTagArticles', tag.name)"
            >
              <VChip size="small" color="secondary" variant="flat">{{ tag.name }}</VChip>
            </button>
          </template>
        </div>

        <div class="tag-management-count-cell">
          <button
            class="tag-management-count-button"
            type="button"
            :aria-label="t('tags.openArticles', { name: tag.name })"
            @click="emit('openTagArticles', tag.name)"
          >
            {{ tag.articleCount || 0 }}
          </button>
        </div>

        <div class="tag-management-actions">
          <VTooltip :text="t('tags.rename')" location="top">
            <template #activator="{ props: tooltipProps }">
              <span v-bind="tooltipProps">
                <VBtn
                  class="tag-management-action"
                  size="small"
                  variant="text"
                  color="primary"
                  :disabled="saving"
                  :aria-label="t('tags.rename')"
                  @click="startRename(tag)"
                >
                  <template #prepend>
                    <Pencil :size="16" />
                  </template>
                  {{ t("tags.rename") }}
                </VBtn>
              </span>
            </template>
          </VTooltip>
          <VTooltip :text="t('tags.merge')" location="top">
            <template #activator="{ props: tooltipProps }">
              <span v-bind="tooltipProps">
                <VBtn
                  class="tag-management-action"
                  size="small"
                  variant="text"
                  color="primary"
                  :disabled="saving || tags.length < 2"
                  :aria-label="t('tags.merge')"
                  @click="openMerge(tag)"
                >
                  <template #prepend>
                    <GitMerge :size="16" />
                  </template>
                  {{ t("tags.merge") }}
                </VBtn>
              </span>
            </template>
          </VTooltip>
          <VTooltip :text="deleteTooltip(tag)" location="top">
            <template #activator="{ props: tooltipProps }">
              <span v-bind="tooltipProps">
                <VBtn
                  class="tag-management-action"
                  size="small"
                  variant="text"
                  color="error"
                  :disabled="saving || (tag.articleCount || 0) > 0"
                  :aria-label="t('tags.deleteTooltip')"
                  @click="requestDelete(tag)"
                >
                  <template #prepend>
                    <Trash2 :size="16" />
                  </template>
                  {{ t("tags.deleteTooltip") }}
                </VBtn>
              </span>
            </template>
          </VTooltip>
        </div>
      </div>

      <div v-if="sortedTags.length === 0" class="tag-management-empty-filter">
        <strong>{{ t("tags.filterEmptyTitle") }}</strong>
        <span>{{ t("tags.filterEmptyBody") }}</span>
      </div>

      <p class="tag-management-help">
        {{ t("tags.help") }}
      </p>
    </div>

    <VDialog :model-value="addDialogOpen" max-width="420" @update:model-value="!$event && closeAddDialog()">
      <VCard class="tag-management-dialog">
        <VCardTitle>{{ t("tags.addTitle") }}</VCardTitle>
        <VCardText class="tag-management-dialog-body">
          <VTextField
            v-model="addDraft"
            :label="t('tags.name')"
            autofocus
            :disabled="saving"
            @keyup.enter="confirmAdd"
          />
        </VCardText>
        <VCardActions>
          <VSpacer />
          <VBtn variant="text" :disabled="saving" @click="closeAddDialog">{{ t("common.cancel") }}</VBtn>
          <VBtn
            class="action-button action-button-primary"
            color="primary"
            variant="flat"
            :loading="saving"
            :disabled="saving || !addDraft.trim()"
            @click="confirmAdd"
          >
            {{ t("common.add") }}
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>

    <VDialog :model-value="Boolean(mergeSource)" max-width="460" @update:model-value="!$event && closeMerge()">
      <VCard class="tag-management-dialog">
        <VCardTitle>{{ t("tags.mergeTitle") }}</VCardTitle>
        <VCardText class="tag-management-dialog-body">
          <p>
            <strong>{{ mergeSource?.name }}</strong>
            {{ t("tags.mergeBody", { name: mergeSource?.name || '' }) }}
          </p>
          <VSelect
            v-model="mergeTargetId"
            class="readstack-select"
            :label="t('tags.mergeTarget')"
            :items="mergeOptions"
            item-title="title"
            item-value="value"
          />
        </VCardText>
        <VCardActions>
          <VSpacer />
          <VBtn variant="text" @click="closeMerge">{{ t("common.cancel") }}</VBtn>
          <VBtn
            class="action-button action-button-primary"
            color="primary"
            variant="flat"
            :loading="saving"
            :disabled="saving || !mergeTargetId"
            @click="confirmMerge"
          >
            {{ t("tags.mergeConfirm") }}
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>

    <VDialog :model-value="Boolean(deleteCandidate)" max-width="420" @update:model-value="!$event && (deleteCandidate = null)">
      <VCard class="tag-management-dialog">
        <VCardTitle>{{ t("tags.deleteUnusedTitle") }}</VCardTitle>
        <VCardText>
          {{ t("tags.deleteUnusedBody", { name: deleteCandidate?.name || '' }) }}
        </VCardText>
        <VCardActions>
          <VSpacer />
          <VBtn variant="text" @click="deleteCandidate = null">{{ t("common.cancel") }}</VBtn>
          <VBtn
            class="action-button action-button-danger"
            color="error"
            variant="flat"
            :loading="saving"
            :disabled="saving"
            @click="confirmDelete"
          >
            {{ t("common.deleteAction") }}
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>
  </section>
</template>
