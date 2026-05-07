<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ArrowLeft, ChevronDown, ExternalLink, Heart, Save, Trash2 } from 'lucide-vue-next'
import DateField from '../../../shared/components/DateField.vue'
import StarRating from '../../../shared/components/StarRating.vue'
import MarkdownViewer from './MarkdownViewer.vue'
import TagEditor from './TagEditor.vue'
import {
  articleToDetailForm,
  createEmptyArticleDetailForm,
  detailFormToArticleInput,
  favoriteToggleInput,
  hasArticleDetailFormChanges
} from '../domain/articleForms'
import type { Article, ArticleInput, ArticleStatus, Tag } from '../types'
import { formatDateTime } from '../../../shared/utils/dateFormat'

const props = withDefaults(defineProps<{
  article?: Article | null
  tags?: Tag[]
  saving?: boolean
  deleting?: boolean
  error?: string
}>(), {
  article: null,
  tags: () => [],
  saving: false,
  deleting: false,
  error: ''
})

const emit = defineEmits<{
  back: []
  save: [article: ArticleInput]
  delete: [articleId: string]
  'update:dirty': [value: boolean]
}>()

const form = reactive(createEmptyArticleDetailForm())
const deleteDialogOpen = ref(false)
const isEditing = ref(false)
const articleDetailsOpen = ref(false)
const submitted = ref(false)
const detailMode = computed<'view' | 'edit'>({
  get: () => (isEditing.value ? 'edit' : 'view'),
  set: (value) => {
    if (value === 'edit') {
      startEditing()
      return
    }

    if (isEditing.value) {
      cancelEditing()
    }
  }
})
const tagOptions = computed(() => [...new Set(props.tags.map((tag) => tag.name).filter(Boolean))])
const statusOptions = [
  { label: '未読', value: 'UNREAD' },
  { label: '既読', value: 'READ' }
] satisfies Array<{ label: string, value: Exclude<ArticleStatus, 'ALL'> }>
const summaryText = computed(() => props.article?.summary?.trim() || '概要はまだありません')
const notesText = computed(() => props.article?.notes?.trim() || '')
const hasUnsavedChanges = computed(() => Boolean(
  props.article && isEditing.value && hasArticleDetailFormChanges(form, props.article)
))
const urlError = computed(() => (form.url.trim() ? '' : 'URLは必須です'))
const titleError = computed(() => (form.title.trim() ? '' : 'タイトルを入力してください'))
const readDateError = computed(() => (form.status === 'READ' && !form.readDate ? '既読にする場合は既読日を入力してください' : ''))
const formValid = computed(() => !urlError.value && !titleError.value && !readDateError.value)

watch(
  () => props.article,
  (article) => {
    Object.assign(form, article ? articleToDetailForm(article) : createEmptyArticleDetailForm())
    isEditing.value = false
    articleDetailsOpen.value = false
    submitted.value = false
  },
  { immediate: true }
)

watch(hasUnsavedChanges, (value) => {
  emit('update:dirty', value)
}, { immediate: true })

function submit(): void {
  submitted.value = true
  if (!formValid.value || props.saving) return
  emit('save', detailFormToArticleInput(form))
}

function startEditing(): void {
  if (!props.article) return
  Object.assign(form, articleToDetailForm(props.article))
  isEditing.value = true
  submitted.value = false
}

function cancelEditing(): void {
  Object.assign(form, props.article ? articleToDetailForm(props.article) : createEmptyArticleDetailForm())
  isEditing.value = false
  submitted.value = false
}

function toggleFavorite(): void {
  if (!props.article) return
  if (isEditing.value) {
    form.favorite = !form.favorite
    return
  }

  emit('save', favoriteToggleInput(props.article))
}

function confirmDelete(): void {
  if (!props.article) return
  if (props.deleting) return
  deleteDialogOpen.value = false
  emit('delete', props.article.id)
}
</script>

<template>
  <main class="detail-page" :class="{ 'is-editing': isEditing }">
    <div v-if="!article" class="empty-detail">
      <span>記事を選択すると詳細を編集できます。</span>
    </div>

    <VForm v-else class="detail-form" @submit.prevent="submit">
      <div class="detail-topbar">
        <VBtn class="action-button action-button-secondary compact-button detail-back-button" variant="outlined" @click="emit('back')">
          <template #prepend>
            <ArrowLeft :size="17" />
          </template>
          戻る
        </VBtn>

        <VBtnToggle v-model="detailMode" class="mode-toggle detail-mode-toggle" mandatory>
          <VBtn value="view">閲覧</VBtn>
          <VBtn value="edit">編集</VBtn>
        </VBtnToggle>

        <div class="detail-topbar-actions">
          <VBtn
            class="favorite-button detail-favorite-button"
            :class="{ 'is-active': isEditing ? form.favorite : article.favorite }"
            icon
            title="お気に入り"
            variant="text"
            @click="toggleFavorite"
          >
            <Heart :size="19" :fill="(isEditing ? form.favorite : article.favorite) ? 'currentColor' : 'none'" />
          </VBtn>
          <VBtn
            color="primary"
            icon
            variant="flat"
            type="submit"
            class="detail-action-icon-button detail-save-button"
            :disabled="!isEditing || props.saving"
            :loading="props.saving"
            title="保存"
            aria-label="保存"
          >
            <Save :size="18" />
          </VBtn>
          <VBtn
            class="detail-action-icon-button detail-delete-button"
            color="error"
            icon
            title="削除"
            aria-label="削除"
            variant="outlined"
            :loading="props.deleting"
            :disabled="props.deleting"
            @click="deleteDialogOpen = true"
          >
            <Trash2 :size="18" />
          </VBtn>
        </div>
      </div>

      <div class="detail-header">
        <div>
          <h2>{{ article.title }}</h2>
          <a :href="article.url" :title="article.url" target="_blank" rel="noreferrer">
            <ExternalLink :size="15" />
            <span class="detail-link-text">{{ article.url }}</span>
          </a>
          <div class="detail-timestamps">
            <span>登録日時 {{ formatDateTime(article.createdAt) }}</span>
            <span>更新日時 {{ formatDateTime(article.updatedAt) }}</span>
          </div>
        </div>
      </div>

      <div class="detail-layout">
        <section class="detail-main">
          <div
            v-if="props.error"
            class="form-error-banner detail-form-error-banner"
            role="alert"
            aria-live="assertive"
          >
            <span>{{ props.error }}</span>
          </div>
          <template v-if="isEditing">
            <section class="detail-section detail-edit-notes-section">
              <div class="detail-section-header detail-notes-header">
                <h3>メモ</h3>
                <span>検索対象になります。Markdown 記法も使えます</span>
              </div>
              <VTextarea
                v-model="form.notes"
                class="detail-edit-notes-field"
                counter="2000"
                label="メモ"
                rows="13"
                variant="outlined"
              />
            </section>

            <section class="detail-section detail-edit-article-section">
              <button
                class="detail-accordion-trigger"
                type="button"
                :aria-expanded="articleDetailsOpen"
                @click="articleDetailsOpen = !articleDetailsOpen"
              >
                <span>記事の詳細</span>
                <ChevronDown :size="18" aria-hidden="true" />
              </button>

              <Transition name="detail-accordion">
                <div v-if="articleDetailsOpen" class="detail-edit-fields">
                  <div class="detail-edit-field-row">
                    <VTextField
                      v-model="form.title"
                      label="タイトル"
                      required
                      variant="outlined"
                      :error-messages="submitted && titleError ? [titleError] : []"
                    />
                  </div>

                  <div class="detail-edit-field-row">
                    <VTextField
                      v-model="form.url"
                      label="URL"
                      type="url"
                      required
                      variant="outlined"
                      :error-messages="submitted && urlError ? [urlError] : []"
                    />
                  </div>

                  <div class="detail-edit-field-row">
                    <VTextarea v-model="form.summary" label="概要" rows="4" auto-grow hide-details variant="outlined" />
                  </div>

                  <div class="detail-edit-field-row detail-edit-tag-row">
                    <div class="detail-edit-subsection-heading">
                      <span>タグ編集</span>
                    </div>
                    <TagEditor
                      v-model="form.tags"
                      class="detail-edit-tag-editor"
                      :options="tagOptions"
                    />
                  </div>
                </div>
              </Transition>
            </section>
          </template>
          <template v-else>
            <section class="detail-section">
              <div class="detail-section-header">
                <h3>概要</h3>
              </div>
              <p class="detail-body-copy" :class="{ 'is-empty': !article.summary }">
                {{ summaryText }}
              </p>
            </section>

            <section class="detail-section">
              <div class="detail-section-header">
                <h3>タグ</h3>
              </div>
              <div v-if="article.tags.length > 0" class="tag-list detail-tag-list">
                <VChip v-for="tag in article.tags" :key="tag.id || tag.name" size="small" color="secondary" variant="flat">
                  {{ tag.name }}
                </VChip>
              </div>
              <p v-else class="detail-body-copy is-empty">タグはまだありません</p>
            </section>

            <section class="detail-section">
              <div class="detail-section-header">
                <h3>メモ</h3>
              </div>
              <MarkdownViewer v-if="article.notes" :source="article.notes" />
              <p v-else class="detail-body-copy detail-notes-copy is-empty">{{ notesText }}</p>
            </section>
          </template>
        </section>

        <VCard class="detail-meta" variant="flat">
          <VCardText class="detail-meta-content">
            <div class="detail-meta-block">
              <span class="detail-meta-label">ステータス</span>
              <VSelect
                v-model="form.status"
                class="readstack-select detail-meta-control"
                :items="statusOptions"
                item-title="label"
                item-value="value"
                density="comfortable"
                :disabled="!isEditing"
                hide-details
                variant="outlined"
              />
            </div>

            <div class="detail-meta-block">
              <span class="detail-meta-label">既読日</span>
              <DateField
                v-model="form.readDate"
                class="readstack-date-field detail-meta-control"
                density="comfortable"
                :disabled="!isEditing"
                :clearable="isEditing"
                :error-messages="submitted && readDateError ? [readDateError] : []"
              />
            </div>

            <div class="detail-meta-block">
              <span class="detail-meta-label">おすすめ度</span>
              <div class="rating-field detail-rating-field">
                <template v-if="isEditing">
                  <StarRating v-model="form.rating" :size="20" />
                </template>
                <template v-else>
                  <StarRating :model-value="article.rating" readonly :size="20" />
                </template>
              </div>
            </div>
          </VCardText>
        </VCard>
      </div>

      <VDialog v-model="deleteDialogOpen" max-width="420">
        <VCard class="delete-confirm-dialog" title="記事を削除しますか？">
          <VCardText>
            <p>
              「{{ article.title }}」を削除します。この操作は取り消せません。
            </p>
          </VCardText>
          <VCardActions>
            <VSpacer />
            <VBtn class="action-button action-button-secondary" variant="outlined" @click="deleteDialogOpen = false">キャンセル</VBtn>
            <VBtn class="action-button action-button-danger" color="error" variant="flat" :loading="props.deleting" :disabled="props.deleting" @click="confirmDelete">削除する</VBtn>
          </VCardActions>
        </VCard>
      </VDialog>
    </VForm>
  </main>
</template>
