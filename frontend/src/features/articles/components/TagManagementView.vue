<script setup lang="ts">
import { computed, ref } from "vue";
import { Check, GitMerge, Pencil, Trash2, X } from "lucide-vue-next";
import type { Tag } from "../types";

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
  retry: [];
}>();

const renameTagId = ref("");
const renameDraft = ref("");
const mergeSource = ref<Tag | null>(null);
const mergeTargetId = ref("");
const deleteCandidate = ref<Tag | null>(null);

const sortedTags = computed(() =>
  [...props.tags].sort((left, right) => {
    const countDiff = (right.articleCount || 0) - (left.articleCount || 0);
    return countDiff || left.name.localeCompare(right.name);
  }),
);

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
</script>

<template>
  <section class="tag-management-view">
    <header class="page-header tag-management-header">
      <div>
        <h1>タグ管理</h1>
      </div>
      <div class="tag-management-summary">
        <span>タグ数</span>
        <strong>{{ tags.length }}</strong>
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
      <h2>タグはまだありません</h2>
      <p>記事にタグを付けると、ここで整理できるようになります。</p>
    </div>

    <div v-else class="tag-management-list" aria-live="polite">
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
            <VChip size="small" color="secondary" variant="flat">{{ tag.name }}</VChip>
          </template>
        </div>

        <div class="tag-management-count-cell">
          <strong>{{ tag.articleCount || 0 }}</strong>
        </div>

        <div class="tag-management-actions">
          <VBtn
            icon
            size="small"
            variant="text"
            color="primary"
            :disabled="saving"
            aria-label="タグ名を変更"
            @click="startRename(tag)"
          >
            <Pencil :size="17" />
          </VBtn>
          <VBtn
            icon
            size="small"
            variant="text"
            color="primary"
            :disabled="saving || tags.length < 2"
            aria-label="タグを統合"
            @click="openMerge(tag)"
          >
            <GitMerge :size="17" />
          </VBtn>
          <VBtn
            icon
            size="small"
            variant="text"
            color="error"
            :disabled="saving || (tag.articleCount || 0) > 0"
            aria-label="未使用タグを削除"
            @click="requestDelete(tag)"
          >
            <Trash2 :size="17" />
          </VBtn>
        </div>
      </div>
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
          <strong>{{ deleteCandidate?.name }}</strong>
          を削除します。
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
