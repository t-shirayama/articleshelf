<script setup lang="ts">
import { computed, ref } from "vue";
import { Check, GitMerge, Pencil, Search, Trash2, X } from "lucide-vue-next";
import type { Tag } from "../types";

type TagSort = "COUNT_DESC" | "COUNT_ASC" | "NAME_ASC";

const props = defineProps<{
  tags: Tag[];
  error: string;
  loading: boolean;
  saving: boolean;
}>();

const emit = defineEmits<{
  renameTag: [id: string, name: string];
  mergeTag: [sourceId: string, targetId: string];
  deleteTag: [id: string];
  openTagArticles: [tagName: string];
  addArticle: [];
  retry: [];
}>();

const renameTagId = ref("");
const renameDraft = ref("");
const mergeSource = ref<Tag | null>(null);
const mergeTargetId = ref("");
const deleteCandidate = ref<Tag | null>(null);
const searchQuery = ref<string | null>("");
const sortMode = ref<TagSort>("COUNT_DESC");

const sortOptions = [
  { title: "記事数が多い順", value: "COUNT_DESC" },
  { title: "記事数が少ない順", value: "COUNT_ASC" },
  { title: "名前順", value: "NAME_ASC" },
];

const filteredTags = computed(() => {
  const keyword = (searchQuery.value || "").trim().toLocaleLowerCase("ja");
  if (!keyword) return props.tags;
  return props.tags.filter((tag) =>
    tag.name.toLocaleLowerCase("ja").includes(keyword),
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
  return (tag.articleCount || 0) > 0 ? "使用中のタグは削除できません" : "削除";
}
</script>

<template>
  <section class="tag-management-view">
    <header class="page-header tag-management-header">
      <div>
        <h1>
          タグ管理
          <span class="tag-management-title-count">{{ tags.length }}件</span>
        </h1>
      </div>
    </header>

    <div
      v-if="error"
      class="error-banner"
      role="alert"
      aria-live="assertive"
    >
      <strong>タグ操作を完了できませんでした</strong>
      <span>{{ error }}</span>
      <div class="error-banner-actions">
        <VBtn
          class="action-button action-button-secondary error-banner-action"
          variant="outlined"
          size="small"
          @click="emit('retry')"
        >
          再試行
        </VBtn>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <VProgressCircular indeterminate color="primary" size="42" />
    </div>

    <div v-else-if="tags.length === 0" class="empty-state">
      <h2>まだタグがありません</h2>
      <p>記事を登録するときにタグを追加すると、ここで整理できます。</p>
      <VBtn
        class="action-button action-button-primary"
        color="primary"
        variant="flat"
        @click="emit('addArticle')"
      >
        記事を追加する
      </VBtn>
    </div>

    <div v-else class="tag-management-list" aria-live="polite">
      <div class="tag-management-toolbar">
        <VTextField
          v-model="searchQuery"
          class="tag-management-search"
          density="compact"
          hide-details
          placeholder="タグ名で検索"
          clearable
        >
          <template #prepend-inner>
            <Search :size="17" />
          </template>
        </VTextField>
        <VSelect
          v-model="sortMode"
          class="readstack-select tag-management-sort"
          density="compact"
          hide-details
          :items="sortOptions"
          item-title="title"
          item-value="value"
          aria-label="タグの並び順"
        />
      </div>

      <div class="tag-management-row tag-management-row-head">
        <span>タグ</span>
        <span>記事数</span>
        <span>操作</span>
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
              aria-label="タグ名を保存"
              @click="confirmRename"
            >
              <Check :size="18" />
            </VBtn>
            <VBtn
              icon
              size="small"
              variant="text"
              aria-label="リネームをキャンセル"
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
            :aria-label="`${tag.name} の記事一覧を開く`"
            @click="emit('openTagArticles', tag.name)"
          >
            {{ tag.articleCount || 0 }}
          </button>
        </div>

        <div class="tag-management-actions">
          <VTooltip text="編集" location="top">
            <template #activator="{ props: tooltipProps }">
              <span v-bind="tooltipProps">
                <VBtn
                  class="tag-management-action"
                  size="small"
                  variant="text"
                  color="primary"
                  :disabled="saving"
                  aria-label="編集"
                  @click="startRename(tag)"
                >
                  <template #prepend>
                    <Pencil :size="16" />
                  </template>
                  編集
                </VBtn>
              </span>
            </template>
          </VTooltip>
          <VTooltip text="タグを統合" location="top">
            <template #activator="{ props: tooltipProps }">
              <span v-bind="tooltipProps">
                <VBtn
                  class="tag-management-action"
                  size="small"
                  variant="text"
                  color="primary"
                  :disabled="saving || tags.length < 2"
                  aria-label="タグを統合"
                  @click="openMerge(tag)"
                >
                  <template #prepend>
                    <GitMerge :size="16" />
                  </template>
                  統合
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
                  aria-label="削除"
                  @click="requestDelete(tag)"
                >
                  <template #prepend>
                    <Trash2 :size="16" />
                  </template>
                  削除
                </VBtn>
              </span>
            </template>
          </VTooltip>
        </div>
      </div>

      <div v-if="sortedTags.length === 0" class="tag-management-empty-filter">
        <strong>条件に一致するタグがありません</strong>
        <span>検索語を変えると、別のタグが見つかるかもしれません。</span>
      </div>

      <p class="tag-management-help">
        タグ名の編集、統合、削除ができます。使用中のタグは削除できません。タグ名や記事数を選ぶと、そのタグの記事一覧へ移動します。
      </p>
    </div>

    <VDialog :model-value="Boolean(mergeSource)" max-width="460" @update:model-value="!$event && closeMerge()">
      <VCard class="tag-management-dialog">
        <VCardTitle>タグを統合</VCardTitle>
        <VCardText class="tag-management-dialog-body">
          <p>
            <strong>{{ mergeSource?.name }}</strong>
            を選択したタグへ統合します。
          </p>
          <VSelect
            v-model="mergeTargetId"
            class="readstack-select"
            label="統合先"
            :items="mergeOptions"
            item-title="title"
            item-value="value"
          />
        </VCardText>
        <VCardActions>
          <VSpacer />
          <VBtn variant="text" @click="closeMerge">キャンセル</VBtn>
          <VBtn
            class="action-button action-button-primary"
            color="primary"
            variant="flat"
            :loading="saving"
            :disabled="saving || !mergeTargetId"
            @click="confirmMerge"
          >
            統合する
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>

    <VDialog :model-value="Boolean(deleteCandidate)" max-width="420" @update:model-value="!$event && (deleteCandidate = null)">
      <VCard class="tag-management-dialog">
        <VCardTitle>未使用タグを削除</VCardTitle>
        <VCardText>
          未使用の
          <strong>「{{ deleteCandidate?.name }}」</strong>
          タグを削除します。このタグは記事に紐づいていません。記事自体は削除されません。
        </VCardText>
        <VCardActions>
          <VSpacer />
          <VBtn variant="text" @click="deleteCandidate = null">キャンセル</VBtn>
          <VBtn
            class="action-button action-button-danger"
            color="error"
            variant="flat"
            :loading="saving"
            :disabled="saving"
            @click="confirmDelete"
          >
            削除する
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>
  </section>
</template>
