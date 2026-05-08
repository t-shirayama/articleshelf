<script setup lang="ts">
import { toRef } from "vue";
import { useI18n } from "vue-i18n";
import { Check, GitMerge, Pencil, Plus, Search, Trash2, X } from "lucide-vue-next";
import TagAddDialog from "./TagAddDialog.vue";
import TagDeleteDialog from "./TagDeleteDialog.vue";
import TagMergeDialog from "./TagMergeDialog.vue";
import { useTagManagementState } from "../composables/useTagManagementState";
import type { Tag } from "../types";

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
const {
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
} = useTagManagementState(toRef(props, "tags"), t);

function confirmAdd(): void {
  const name = addDraft.value.trim();
  if (!name) return;
  emit("addTag", name);
  addDraft.value = "";
  addDialogOpen.value = false;
}

function confirmRename(): void {
  const name = renameDraft.value.trim();
  if (!renameTagId.value || !name) return;
  emit("renameTag", renameTagId.value, name);
  cancelRename();
}

function confirmMerge(): void {
  if (!mergeSource.value?.id || !mergeTargetId.value) return;
  emit("mergeTag", mergeSource.value.id, mergeTargetId.value);
  closeMerge();
}

function confirmDelete(): void {
  if (!deleteCandidate.value?.id) return;
  emit("deleteTag", deleteCandidate.value.id);
  deleteCandidate.value = null;
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
        <span ref="sortMeasureEl" class="tag-management-sort-measure" aria-hidden="true">
          {{ longestSortTitle }}
        </span>
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
          class="articleshelf-select sort-select tag-management-sort"
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

    <TagAddDialog
      v-model:draft="addDraft"
      :open="addDialogOpen"
      :saving="saving"
      @cancel="closeAddDialog(saving)"
      @confirm="confirmAdd"
    />

    <TagMergeDialog
      v-model:target-id="mergeTargetId"
      :source="mergeSource"
      :options="mergeOptions"
      :saving="saving"
      @cancel="closeMerge"
      @confirm="confirmMerge"
    />

    <TagDeleteDialog
      :candidate="deleteCandidate"
      :saving="saving"
      @cancel="deleteCandidate = null"
      @confirm="confirmDelete"
    />
  </section>
</template>
