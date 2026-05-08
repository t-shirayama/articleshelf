<script setup lang="ts">
import { ref, toRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, ChevronDown, ExternalLink, Eye, Heart, Pencil, Save, Trash2 } from 'lucide-vue-next'
import ArticleDeleteDialog from './ArticleDeleteDialog.vue'
import ArticleDetailMetaPanel from './ArticleDetailMetaPanel.vue'
import MarkdownViewer from './MarkdownViewer.vue'
import TagEditor from './TagEditor.vue'
import { useArticleDetailForm } from '../composables/useArticleDetailForm'
import type { Article, ArticleInput, Tag } from '../types'
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

const { t } = useI18n()
const notesPreviewOpen = ref(false)
const {
  form,
  deleteDialogOpen,
  isEditing,
  articleDetailsOpen,
  submitted,
  detailMode,
  tagOptions,
  statusOptions,
  summaryText,
  notesText,
  urlError,
  titleError,
  readDateError,
  createSubmitInput,
  createFavoriteInput,
} = useArticleDetailForm(
  toRef(props, 'article'),
  toRef(props, 'tags'),
  t,
  (value) => emit('update:dirty', value),
)

function submit(): void {
  const input = createSubmitInput(props.saving)
  if (!input) return
  emit('save', input)
}

function toggleFavorite(): void {
  const input = createFavoriteInput()
  if (!input) return
  emit('save', input)
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
      <span>{{ t('detail.emptySelection') }}</span>
    </div>

    <VForm v-else class="detail-form" @submit.prevent="submit">
      <div class="detail-topbar">
        <VBtn class="action-button action-button-secondary compact-button detail-back-button" variant="outlined" @click="emit('back')">
          <template #prepend>
            <ArrowLeft :size="17" />
          </template>
          {{ t('common.back') }}
        </VBtn>

        <VBtnToggle v-model="detailMode" class="mode-toggle detail-mode-toggle" mandatory>
          <VBtn value="view">{{ t('detail.view') }}</VBtn>
          <VBtn value="edit">{{ t('detail.edit') }}</VBtn>
        </VBtnToggle>

        <div class="detail-topbar-actions">
          <VBtn
            class="favorite-button detail-favorite-button"
            :class="{ 'is-active': isEditing ? form.favorite : article.favorite }"
            icon
            :title="t('common.favorite')"
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
            :title="t('common.save')"
            :aria-label="t('common.save')"
          >
            <Save :size="18" />
          </VBtn>
          <VBtn
            class="detail-action-icon-button detail-delete-button"
            color="error"
            icon
            :title="t('common.delete')"
            :aria-label="t('common.delete')"
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
            <span>{{ t('common.createdAt') }} {{ formatDateTime(article.createdAt) }}</span>
            <span>{{ t('common.updatedAt') }} {{ formatDateTime(article.updatedAt) }}</span>
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
                <div class="detail-notes-heading-copy">
                  <h3>{{ t('common.notes') }}</h3>
                  <span>{{ t('detail.notesHelp') }}</span>
                </div>
                <VBtn
                  class="detail-notes-preview-button"
                  variant="outlined"
                  color="primary"
                  size="small"
                  type="button"
                  @click="notesPreviewOpen = !notesPreviewOpen"
                >
                  <template #prepend>
                    <Pencil v-if="notesPreviewOpen" :size="16" />
                    <Eye v-else :size="16" />
                  </template>
                  {{ notesPreviewOpen ? t('detail.notesEdit') : t('detail.notesPreview') }}
                </VBtn>
              </div>
              <div v-if="notesPreviewOpen" class="detail-notes-preview">
                <MarkdownViewer v-if="form.notes.trim()" :source="form.notes" />
                <p v-else class="detail-body-copy detail-notes-copy is-empty">{{ notesText }}</p>
              </div>
              <VTextarea
                v-else
                v-model="form.notes"
                class="detail-edit-notes-field"
                counter="2000"
                :label="t('common.notes')"
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
                <span>{{ t('detail.articleDetails') }}</span>
                <ChevronDown :size="18" aria-hidden="true" />
              </button>

              <Transition name="detail-accordion">
                <div v-if="articleDetailsOpen" class="detail-edit-fields">
                  <div class="detail-edit-field-row">
                    <VTextField
                      v-model="form.title"
                      :label="t('common.title')"
                      required
                      variant="outlined"
                      :error-messages="submitted && titleError ? [titleError] : []"
                    />
                  </div>

                  <div class="detail-edit-field-row">
                    <VTextField
                      v-model="form.url"
                      :label="t('common.url')"
                      type="url"
                      required
                      variant="outlined"
                      :error-messages="submitted && urlError ? [urlError] : []"
                    />
                  </div>

                  <div class="detail-edit-field-row">
                    <VTextarea v-model="form.summary" :label="t('common.summary')" rows="4" auto-grow hide-details variant="outlined" />
                  </div>

                  <div class="detail-edit-field-row detail-edit-tag-row">
                    <div class="detail-edit-subsection-heading">
                      <span>{{ t('detail.tagEdit') }}</span>
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
                <h3>{{ t('common.summary') }}</h3>
              </div>
              <p class="detail-body-copy" :class="{ 'is-empty': !article.summary }">
                {{ summaryText }}
              </p>
            </section>

            <section class="detail-section">
              <div class="detail-section-header">
                <h3>{{ t('common.tags') }}</h3>
              </div>
              <div v-if="article.tags.length > 0" class="tag-list detail-tag-list">
                <VChip v-for="tag in article.tags" :key="tag.id || tag.name" size="small" color="secondary" variant="flat">
                  {{ tag.name }}
                </VChip>
              </div>
              <p v-else class="detail-body-copy is-empty">{{ t('detail.emptyTags') }}</p>
            </section>

            <section class="detail-section">
              <div class="detail-section-header">
                <h3>{{ t('common.notes') }}</h3>
              </div>
              <MarkdownViewer v-if="article.notes" :source="article.notes" />
              <p v-else class="detail-body-copy detail-notes-copy is-empty">{{ notesText }}</p>
            </section>
          </template>
        </section>

        <ArticleDetailMetaPanel
          :form="form"
          :article-rating="article.rating"
          :is-editing="isEditing"
          :submitted="submitted"
          :status-options="statusOptions"
          :read-date-error="readDateError"
        />
      </div>

      <ArticleDeleteDialog
        v-model:open="deleteDialogOpen"
        :title="article.title"
        :deleting="props.deleting"
        @confirm="confirmDelete"
      />
    </VForm>
  </main>
</template>
