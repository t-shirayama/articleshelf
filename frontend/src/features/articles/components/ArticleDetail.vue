<script setup lang="ts">
import { ref, toRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, ChevronDown, ExternalLink, Heart, Save, Trash2 } from 'lucide-vue-next'
import ArticleDeleteDialog from './ArticleDeleteDialog.vue'
import ArticleDetailMetaPanel from './ArticleDetailMetaPanel.vue'
import ArticleDetailViewSections from './ArticleDetailViewSections.vue'
import ArticleNotesEditor from './ArticleNotesEditor.vue'
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
  errorActionLabel?: string
}>(), {
  article: null,
  tags: () => [],
  saving: false,
  deleting: false,
  error: '',
  errorActionLabel: ''
})

const emit = defineEmits<{
  back: []
  save: [article: ArticleInput]
  delete: [articleId: string]
  errorAction: []
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
  <section class="detail-page" :class="{ 'is-editing': isEditing }">
    <div v-if="!article" class="empty-detail">
      <span>{{ t('detail.emptySelection') }}</span>
    </div>

    <VForm v-else class="detail-form" :aria-busy="props.saving || props.deleting" @submit.prevent="submit">
      <span v-if="props.saving" class="sr-only" role="status" aria-live="polite">
        {{ t('common.saving') }}
      </span>
      <div class="detail-topbar">
        <VBtn class="action-button action-button-secondary compact-button detail-back-button" variant="outlined" :disabled="props.saving || props.deleting" @click="emit('back')">
          <template #prepend>
            <ArrowLeft :size="17" />
          </template>
          {{ t('common.back') }}
        </VBtn>

        <VBtnToggle v-model="detailMode" class="mode-toggle detail-mode-toggle" mandatory>
          <VBtn value="view" :disabled="props.saving || props.deleting">{{ t('detail.view') }}</VBtn>
          <VBtn value="edit" :disabled="props.saving || props.deleting">{{ t('detail.edit') }}</VBtn>
        </VBtnToggle>

        <div class="detail-topbar-actions">
          <VBtn
            class="favorite-button detail-favorite-button"
            :class="{ 'is-active': isEditing ? form.favorite : article.favorite }"
            icon
            :title="t('common.favorite')"
            variant="text"
            :disabled="props.saving || props.deleting"
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
            :title="props.saving ? t('common.saving') : t('common.save')"
            :aria-label="props.saving ? t('common.saving') : t('common.save')"
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
            :disabled="props.saving || props.deleting"
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
            <VBtn
              v-if="props.errorActionLabel"
              class="detail-error-action"
              color="error"
              variant="text"
              @click="emit('errorAction')"
            >
              {{ props.errorActionLabel }}
            </VBtn>
          </div>
          <template v-if="isEditing">
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
                      :disabled="props.saving"
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
                      :disabled="props.saving"
                      :error-messages="submitted && urlError ? [urlError] : []"
                    />
                  </div>

                  <div class="detail-edit-field-row">
                    <VTextarea v-model="form.summary" :label="t('common.summary')" rows="4" auto-grow hide-details variant="outlined" :disabled="props.saving" />
                  </div>

                  <div class="detail-edit-field-row detail-edit-tag-row">
                    <div class="detail-edit-subsection-heading">
                      <span>{{ t('detail.tagEdit') }}</span>
                    </div>
                    <TagEditor
                      v-model="form.tags"
                      class="detail-edit-tag-editor"
                      :options="tagOptions"
                      :disabled="props.saving"
                    />
                  </div>
                </div>
              </Transition>
            </section>

            <ArticleNotesEditor v-model:preview-open="notesPreviewOpen" :form="form" :notes-text="notesText" :disabled="props.saving" />
          </template>
          <template v-else>
            <ArticleDetailViewSections :article="{ ...article, notes: form.notes }" :summary-text="summaryText" :notes-text="notesText" />
          </template>
        </section>

        <ArticleDetailMetaPanel
          :form="form"
          :article-rating="article.rating"
          :is-editing="isEditing"
          :submitted="submitted"
          :status-options="statusOptions"
          :read-date-error="readDateError"
          :saving="props.saving"
        />
      </div>

      <ArticleDeleteDialog
        v-model:open="deleteDialogOpen"
        :title="article.title"
        :deleting="props.deleting"
        @confirm="confirmDelete"
      />
    </VForm>
  </section>
</template>
