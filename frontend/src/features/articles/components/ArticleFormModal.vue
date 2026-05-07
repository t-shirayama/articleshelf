<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import DateField from '../../../shared/components/DateField.vue'
import StarRating from '../../../shared/components/StarRating.vue'
import TagEditor from './TagEditor.vue'
import { createEmptyArticleCreateForm, createFormToArticleInput } from '../domain/articleForms'
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
const form = reactive(createEmptyArticleCreateForm())
const submitted = ref(false)
const urlInput = ref<{ focus: () => void } | null>(null)
const tagOptions = computed(() => [...new Set(props.tags.map((tag) => tag.name).filter(Boolean))])
const urlError = computed(() => (form.url.trim() ? '' : t('articleForm.validation.urlRequired')))
const readDateError = computed(() => (!form.readLater && !form.readDate ? t('articleForm.validation.readDateRequired') : ''))
const formValid = computed(() => !urlError.value && !readDateError.value)

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
  if (props.saving) return
  submitted.value = false
  emit('close')
}

function handleDialogUpdate(open: boolean): void {
  if (!open) cancel()
}

function submit(): void {
  submitted.value = true
  if (!formValid.value || props.saving) return

  emit('submit', createFormToArticleInput(form))
}
</script>

<template>
  <VDialog :model-value="props.open" max-width="640" @update:model-value="handleDialogUpdate">
    <VCard class="article-modal">
      <header class="article-modal-header">
        <h2>{{ t('articleForm.titleAdd') }}</h2>
        <div class="article-modal-header-actions">
          <VBtn class="action-button action-button-secondary" type="button" variant="outlined" :disabled="props.saving" @click.stop.prevent="cancel">{{ t('common.close') }}</VBtn>
          <VBtn class="action-button action-button-primary" color="primary" variant="flat" type="button" :loading="props.saving" :disabled="props.saving" @click="submit">{{ t('common.saveArticle') }}</VBtn>
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
          />
        </div>

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
            class="readstack-date-field"
            :label="t('common.readDate')"
            :disabled="form.readLater"
            clearable
            :error-messages="submitted && readDateError ? [readDateError] : []"
          />
        </div>

        <div class="modal-field">
          <VTextarea
            v-model="form.notes"
            :label="t('common.notes')"
            rows="5"
            auto-grow
            hide-details
            :placeholder="t('articleForm.notesPlaceholder')"
          />
        </div>
      </VCardText>
    </VCard>
  </VDialog>
</template>
