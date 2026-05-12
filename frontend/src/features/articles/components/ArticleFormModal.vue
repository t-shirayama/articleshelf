<script setup lang="ts">
import { ref, toRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChevronDown } from 'lucide-vue-next'
import DateField from '../../../shared/components/DateField.vue'
import StarRating from '../../../shared/components/StarRating.vue'
import TagEditor from './TagEditor.vue'
import { useArticleCreateForm } from '../composables/useArticleCreateForm'
import { useArticlePreview } from '../composables/useArticlePreview'
import type { ArticleInput, Tag } from '../types'

const props = withDefaults(defineProps<{
  open?: boolean
  tags?: Tag[]
  error?: string
  duplicateArticleId?: string
  saving?: boolean
}>(), {
  open: false,
  tags: () => [],
  error: '',
  duplicateArticleId: '',
  saving: false
})

const emit = defineEmits<{
  close: []
  'open-duplicate': [articleId: string]
  submit: [article: ArticleInput]
}>()

const { t } = useI18n()
const { form, submitted, urlInput, tagOptions, urlError, readDateError, createSubmitInput } = useArticleCreateForm(
  toRef(props, 'open'),
  toRef(props, 'tags'),
  t,
)
const {
  preview,
  duplicateArticleId: previewDuplicateArticleId,
  previewError,
  loading: previewLoading,
  saveDisabledByPreview,
  requestPreview,
  schedulePreview,
  reset: resetPreview,
} = useArticlePreview(toRef(form, 'url'), t)
const detailsPanel = ref<string | null>(null)

watch(() => props.open, (open) => {
  if (!open) {
    resetPreview()
    detailsPanel.value = null
  }
})

function cancel(): void {
  if (props.saving) return
  submitted.value = false
  emit('close')
}

function handleDialogUpdate(open: boolean): void {
  if (!open) cancel()
}

function submit(): void {
  if (saveDisabledByPreview.value) return
  const input = createSubmitInput(props.saving)
  if (!input) return
  emit('submit', input)
}

function toggleDetailsPanel(): void {
  detailsPanel.value = detailsPanel.value === 'details' ? null : 'details'
}
</script>

<template>
  <VDialog :model-value="props.open" max-width="640" content-class="article-modal-overlay" @update:model-value="handleDialogUpdate">
    <VCard class="article-modal">
      <header class="article-modal-header">
        <h2>{{ t('articleForm.titleAdd') }}</h2>
        <div class="article-modal-header-actions">
          <VBtn class="action-button action-button-secondary" type="button" variant="outlined" :disabled="props.saving" @click.stop.prevent="cancel">{{ t('common.close') }}</VBtn>
          <VBtn class="action-button action-button-primary" color="primary" variant="flat" type="button" :loading="props.saving" :disabled="props.saving || saveDisabledByPreview" @click="submit">{{ t('common.saveArticle') }}</VBtn>
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
            {{ t('articles.duplicateOpen') }}
          </VBtn>
        </div>

        <div class="modal-field">
          <VTextField
            ref="urlInput"
            v-model="form.url"
            :label="t('common.url')"
            type="url"
            :placeholder="t('articleForm.urlPlaceholder')"
            hide-details="auto"
            :error-messages="submitted && urlError ? [urlError] : []"
            @paste="schedulePreview"
            @blur="requestPreview"
          />
        </div>

        <div v-if="previewLoading || preview || previewError" class="article-preview-panel" :class="{ 'is-warning': preview && !preview.previewAvailable, 'is-duplicate': previewDuplicateArticleId }" aria-live="polite">
          <div v-if="previewLoading" class="article-preview-loading">
            {{ t('articleForm.previewLoading') }}
          </div>
          <template v-else-if="previewDuplicateArticleId">
            <div class="article-preview-copy">
              <strong>{{ t('articleForm.previewDuplicate') }}</strong>
              <span>{{ t('articleForm.previewDuplicateHelp') }}</span>
            </div>
            <VBtn
              class="action-button action-button-primary duplicate-article-link"
              color="primary"
              size="small"
              type="button"
              variant="flat"
              @click="emit('open-duplicate', previewDuplicateArticleId)"
            >
              {{ t('articles.duplicateOpen') }}
            </VBtn>
          </template>
          <template v-else-if="preview">
            <img v-if="preview.thumbnailUrl" class="article-preview-thumbnail" :src="preview.thumbnailUrl" :alt="t('articleForm.previewThumbnailAlt')" loading="lazy">
            <div class="article-preview-copy">
              <strong>{{ preview.previewAvailable ? preview.title || preview.url : t('articleForm.previewUnavailableTitle') }}</strong>
              <span v-if="preview.previewAvailable">{{ preview.summary || preview.url }}</span>
              <span v-else>{{ t('articleForm.previewUnavailableBody') }}</span>
            </div>
          </template>
          <template v-else>
            <div class="article-preview-copy">
              <strong>{{ t('articleForm.previewFailedTitle') }}</strong>
              <span>{{ previewError }}</span>
            </div>
          </template>
        </div>

        <section class="article-form-details">
          <button
            class="article-form-details-trigger"
            type="button"
            :aria-expanded="detailsPanel === 'details'"
            @click="toggleDetailsPanel"
          >
            <div class="article-form-details-title">
              <span class="article-form-details-copy">
                <strong>{{ t('articleForm.detailsToggle') }}</strong>
                <small>{{ t('articleForm.detailsHelp') }}</small>
              </span>
              <ChevronDown
                class="article-form-details-icon"
                :class="{ 'is-open': detailsPanel === 'details' }"
                :size="18"
                aria-hidden="true"
              />
            </div>
          </button>

          <Transition name="detail-accordion">
            <div v-if="detailsPanel === 'details'" class="article-form-details-content">
              <div class="modal-field title-input-group">
                <VTextField
                  v-model="form.title"
                  :label="t('articleForm.titleOptional')"
                  hide-details
                  :placeholder="t('articleForm.titlePlaceholder')"
                />
                <p>{{ t('articleForm.titleHelp') }}</p>
              </div>

              <div class="modal-field modal-tag-field">
                <div class="modal-subsection-heading">
                  <span>{{ t('common.tags') }}</span>
                </div>
                <TagEditor
                  v-model="form.tags"
                  :options="tagOptions"
                />
              </div>

              <div class="modal-field rating-field">
                <div class="modal-subsection-heading">
                  <span>{{ t('common.rating') }}</span>
                </div>
                <StarRating v-model="form.rating" />
              </div>

              <div class="modal-field field-row modal-status-row">
                <label class="read-later-check">
                  <input v-model="form.readLater" type="checkbox">
                  <span class="read-later-box" aria-hidden="true" />
                  <span class="read-later-copy">
                    <strong>{{ t('articleForm.readLater') }}</strong>
                    <small>{{ t('articleForm.readLaterHelp') }}</small>
                  </span>
                </label>
                <DateField
                  v-model="form.readDate"
                  class="articleshelf-date-field"
                  :label="t('common.readDate')"
                  :disabled="form.readLater"
                  clearable
                  :error-messages="submitted && readDateError ? [readDateError] : []"
                />
              </div>

              <div class="modal-field">
                <VTextarea
                  v-model="form.notes"
                  counter="20000"
                  :label="t('common.notes')"
                  rows="5"
                  auto-grow
                  hide-details
                  :placeholder="t('articleForm.notesPlaceholder')"
                />
              </div>
            </div>
          </Transition>
        </section>
      </VCardText>
    </VCard>
  </VDialog>
</template>
