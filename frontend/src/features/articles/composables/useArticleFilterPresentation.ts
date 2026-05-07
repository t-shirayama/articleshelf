import { computed, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ArticleFilters } from '../types'

export function useArticleFilterPresentation(filters: Ref<ArticleFilters>) {
  const { t } = useI18n()
  const activeFilterSummary = computed<string[]>(() => {
    const summary: string[] = []

    if (filters.value.tags.length > 0) {
      summary.push(
        filters.value.tags.length === 1
          ? t('filters.tagSummary', { tag: filters.value.tags[0] })
          : t('filters.tagSummaryCount', { count: filters.value.tags.length }),
      )
    }

    if (filters.value.ratings.length > 0) {
      const ratingText = filters.value.ratings
        .slice()
        .sort((left, right) => left - right)
        .map((rating) => (rating === 0 ? t('filters.unrated') : `${rating}`))
        .join(', ')
      summary.push(t('filters.ratingSummary', { rating: ratingText }))
    }

    if (filters.value.createdRange.from || filters.value.createdRange.to) {
      summary.push(
        t('filters.createdSummary', { range: formatRange(filters.value.createdRange.from, filters.value.createdRange.to, t) }),
      )
    }

    if (filters.value.readRange.from || filters.value.readRange.to) {
      summary.push(
        t('filters.readSummary', { range: formatRange(filters.value.readRange.from, filters.value.readRange.to, t) }),
      )
    }

    return summary
  })

  const activeFilterCount = computed(() => activeFilterSummary.value.length)
  const pageTitle = computed(() => {
    if (filters.value.favorite) return t('articles.pageTitle.favorite')
    if (filters.value.status === 'UNREAD') return t('articles.pageTitle.unread')
    if (filters.value.status === 'READ') return t('articles.pageTitle.read')
    if (
      filters.value.tags.length === 1 &&
      filters.value.ratings.length === 0 &&
      !filters.value.createdRange.from &&
      !filters.value.createdRange.to &&
      !filters.value.readRange.from &&
      !filters.value.readRange.to
    ) {
      return filters.value.tags[0]
    }
    if (activeFilterSummary.value.length > 0) return t('articles.pageTitle.filtered')
    return t('articles.pageTitle.all')
  })

  return {
    activeFilterCount,
    activeFilterSummary,
    pageTitle
  }
}

function formatRange(from: string, to: string, t: (key: string, params?: Record<string, unknown>) => string): string {
  if (from && to) return t('filters.range', { from, to })
  if (from) return t('filters.from', { date: from })
  if (to) return t('filters.to', { date: to })
  return ''
}
