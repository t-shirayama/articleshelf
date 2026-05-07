import { computed, type Ref } from 'vue'
import type { ArticleFilters } from '../types'

export function useArticleFilterPresentation(filters: Ref<ArticleFilters>) {
  const activeFilterSummary = computed<string[]>(() => {
    const summary: string[] = []

    if (filters.value.tags.length > 0) {
      summary.push(
        filters.value.tags.length === 1
          ? `タグ: ${filters.value.tags[0]}`
          : `タグ: ${filters.value.tags.length}件`,
      )
    }

    if (filters.value.ratings.length > 0) {
      const ratingText = filters.value.ratings
        .slice()
        .sort((left, right) => left - right)
        .map((rating) => (rating === 0 ? '未設定' : `${rating}`))
        .join(', ')
      summary.push(`おすすめ度: ${ratingText}`)
    }

    if (filters.value.createdRange.from || filters.value.createdRange.to) {
      summary.push(
        `登録日: ${formatRange(filters.value.createdRange.from, filters.value.createdRange.to)}`,
      )
    }

    if (filters.value.readRange.from || filters.value.readRange.to) {
      summary.push(
        `既読日: ${formatRange(filters.value.readRange.from, filters.value.readRange.to)}`,
      )
    }

    return summary
  })

  const activeFilterCount = computed(() => activeFilterSummary.value.length)
  const pageTitle = computed(() => {
    if (filters.value.favorite) return 'お気に入り'
    if (filters.value.status === 'UNREAD') return '未読'
    if (filters.value.status === 'READ') return '既読'
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
    if (activeFilterSummary.value.length > 0) return '絞り込み結果'
    return 'すべての記事'
  })

  return {
    activeFilterCount,
    activeFilterSummary,
    pageTitle
  }
}

function formatRange(from: string, to: string): string {
  if (from && to) return `${from} - ${to}`
  if (from) return `${from} 以降`
  if (to) return `${to} 以前`
  return ''
}
