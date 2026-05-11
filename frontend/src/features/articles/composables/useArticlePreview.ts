import { computed, ref, watch, type Ref } from 'vue'
import { ApiRequestError } from '../../../shared/api/client'
import { articlesApi } from '../api/articlesApi'
import type { ArticlePreview } from '../types'

type Translate = (key: string) => string

export function useArticlePreview(url: Ref<string>, t: Translate) {
  const preview = ref<ArticlePreview | null>(null)
  const previewedUrl = ref('')
  const duplicateArticleId = ref('')
  const previewError = ref('')
  const loading = ref(false)
  let requestSequence = 0

  const trimmedUrl = computed(() => url.value.trim())
  const hasDuplicate = computed(() => Boolean(duplicateArticleId.value))
  const hasPreviewForCurrentUrl = computed(() => Boolean(previewedUrl.value && previewedUrl.value === trimmedUrl.value))
  const saveDisabledByPreview = computed(() => hasDuplicate.value && hasPreviewForCurrentUrl.value)

  watch(trimmedUrl, (currentUrl) => {
    if (!currentUrl || currentUrl !== previewedUrl.value) {
      clearPreviewState()
    }
  })

  async function requestPreview(): Promise<void> {
    const requestedUrl = trimmedUrl.value
    if (!requestedUrl || (requestedUrl === previewedUrl.value && (preview.value || duplicateArticleId.value))) return

    const sequence = ++requestSequence
    loading.value = true
    clearPreviewState()

    try {
      const result = await articlesApi.previewArticle(requestedUrl)
      if (!isCurrent(sequence, requestedUrl)) return
      preview.value = result
      previewedUrl.value = result.url
    } catch (error: unknown) {
      if (!isCurrent(sequence, requestedUrl)) return
      if (error instanceof ApiRequestError && error.status === 409 && error.existingArticleId) {
        duplicateArticleId.value = error.existingArticleId
        previewedUrl.value = requestedUrl
        previewError.value = t('articleForm.previewDuplicate')
        return
      }
      previewError.value = t('articleForm.previewFailed')
    } finally {
      if (sequence === requestSequence) {
        loading.value = false
      }
    }
  }

  function schedulePreview(): void {
    window.setTimeout(() => {
      void requestPreview()
    }, 0)
  }

  function reset(): void {
    requestSequence += 1
    loading.value = false
    clearPreviewState()
  }

  function clearPreviewState(): void {
    preview.value = null
    previewedUrl.value = ''
    duplicateArticleId.value = ''
    previewError.value = ''
  }

  function isCurrent(sequence: number, requestedUrl: string): boolean {
    return sequence === requestSequence && requestedUrl === trimmedUrl.value
  }

  return {
    preview,
    previewedUrl,
    duplicateArticleId,
    previewError,
    loading,
    hasPreviewForCurrentUrl,
    saveDisabledByPreview,
    requestPreview,
    schedulePreview,
    reset,
  }
}
