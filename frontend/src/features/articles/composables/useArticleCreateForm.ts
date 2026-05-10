import { computed, nextTick, reactive, ref, watch, type Ref } from 'vue'
import { createEmptyArticleCreateForm, createFormToArticleInput } from '../domain/articleForms'
import type { ArticleInput, Tag } from '../types'

type Translate = (key: string) => string

export function useArticleCreateForm(
  open: Ref<boolean>,
  tags: Ref<Tag[]>,
  t: Translate,
) {
  const form = reactive(createEmptyArticleCreateForm())
  const submitted = ref(false)
  const urlInput = ref<{ focus: () => void } | null>(null)
  const tagOptions = computed(() => [...new Set(tags.value.map((tag) => tag.name).filter(Boolean))])
  const urlError = computed(() => (form.url.trim() ? '' : t('articleForm.validation.urlRequired')))
  const readDateError = computed(() => (!form.readLater && !form.readDate ? t('articleForm.validation.readDateRequired') : ''))
  const formValid = computed(() => !urlError.value && !readDateError.value)

  watch(open, (isOpen) => {
    if (!isOpen) return
    reset()
    focusUrlInput()
  })

  function reset(): void {
    Object.assign(form, createEmptyArticleCreateForm())
    submitted.value = false
  }

  function focusUrlInput(): void {
    nextTick(() => {
      window.requestAnimationFrame(() => {
        urlInput.value?.focus()
      })
    })
  }

  function createSubmitInput(saving: boolean): ArticleInput | null {
    submitted.value = true
    if (!formValid.value || saving) return null
    return createFormToArticleInput(form)
  }

  return {
    form,
    submitted,
    urlInput,
    tagOptions,
    urlError,
    readDateError,
    reset,
    createSubmitInput,
  }
}
