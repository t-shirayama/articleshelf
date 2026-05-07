<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import StarRating from '../../../shared/components/StarRating.vue'
import TagEditor from './TagEditor.vue'
import { createEmptyArticleCreateForm, createFormToArticleInput } from '../domain/articleForms'
import type { ArticleInput, Tag } from '../types'

const props = withDefaults(defineProps<{
  open?: boolean
  tags?: Tag[]
  error?: string
  duplicateArticleId?: string
}>(), {
  open: false,
  tags: () => [],
  error: '',
  duplicateArticleId: ''
})

const emit = defineEmits<{
  close: []
  'open-duplicate': [articleId: string]
  submit: [article: ArticleInput]
}>()

const form = reactive(createEmptyArticleCreateForm())
const submitted = ref(false)
const urlInput = ref<{ focus: () => void } | null>(null)
const tagOptions = computed(() => [...new Set(props.tags.map((tag) => tag.name).filter(Boolean))])
const urlError = computed(() => (form.url.trim() ? '' : 'URLは必須です'))

watch(
  () => props.open,
  (open) => {
    if (open) {
      Object.assign(form, createEmptyArticleCreateForm())
      submitted.value = false
      focusUrlInput()
    }
  }
)

function focusUrlInput(): void {
  nextTick(() => {
    window.requestAnimationFrame(() => {
      urlInput.value?.focus()
    })
  })
}

function cancel(): void {
  submitted.value = false
  emit('close')
}

function handleDialogUpdate(open: boolean): void {
  if (!open) cancel()
}

function submit(): void {
  submitted.value = true
  if (urlError.value) return

  emit('submit', createFormToArticleInput(form))
}
</script>

<template>
  <VDialog :model-value="props.open" max-width="640" @update:model-value="handleDialogUpdate">
    <VCard class="article-modal">
      <header class="article-modal-header">
        <h2>記事を追加</h2>
        <div class="article-modal-header-actions">
          <VBtn class="action-button action-button-secondary" type="button" variant="outlined" @click.stop.prevent="cancel">閉じる</VBtn>
          <VBtn class="action-button action-button-primary" color="primary" variant="flat" type="button" @click="submit">保存する</VBtn>
        </div>
      </header>

      <VCardText class="article-modal-body">
        <div v-if="props.error" class="form-error-banner" role="alert" aria-live="assertive">
          <span>{{ props.error }}</span>
          <VBtn
            v-if="props.duplicateArticleId"
            class="action-button action-button-primary duplicate-article-link"
            color="primary"
            size="small"
            type="button"
            variant="flat"
            @click="emit('open-duplicate', props.duplicateArticleId)"
          >
            登録済みの記事を開く
          </VBtn>
        </div>

        <div class="modal-field">
          <VTextField
            ref="urlInput"
            v-model="form.url"
            label="URL"
            type="url"
            placeholder="https://example.com/article"
            hide-details="auto"
            :error-messages="submitted && urlError ? [urlError] : []"
          />
        </div>

        <div class="modal-field title-input-group">
          <VTextField
            v-model="form.title"
            label="タイトル（任意）"
            hide-details
            placeholder="例: Vue 3 の状態管理を学ぶ"
          />
          <p>未入力の場合はURLから取得した記事タイトルを自動設定します</p>
        </div>

        <div class="modal-field modal-tag-field">
          <div class="modal-subsection-heading">
            <span>タグ選択</span>
          </div>
          <TagEditor
            v-model="form.tags"
            :options="tagOptions"
          />
        </div>

        <div class="modal-field rating-field">
          <div class="modal-subsection-heading">
            <span>おすすめ度</span>
          </div>
          <StarRating v-model="form.rating" />
        </div>

        <div class="modal-field field-row modal-status-row">
          <label class="read-later-check">
            <input v-model="form.readLater" type="checkbox">
            <span class="read-later-box" aria-hidden="true" />
            <span class="read-later-copy">
              <strong>あとで読む</strong>
              <small>チェック中は未読として保存します</small>
            </span>
          </label>
          <VTextField
            v-model="form.readDate"
            class="readstack-date-field"
            label="既読日"
            type="date"
            :disabled="form.readLater"
            clearable
            hide-details
          />
        </div>

        <div class="modal-field">
          <VTextarea
            v-model="form.notes"
            label="メモ"
            rows="5"
            auto-grow
            hide-details
            placeholder="読んだポイントや次に試したいこと"
          />
        </div>
      </VCardText>
    </VCard>
  </VDialog>
</template>
