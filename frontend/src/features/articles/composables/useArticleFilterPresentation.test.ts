import { afterEach, describe, expect, it } from 'vitest'
import { createApp, ref } from 'vue'
import { i18n, setCurrentLocale } from '../../../shared/i18n'
import { createDefaultArticleFilters } from '../domain/articleFilters'
import { useArticleFilterPresentation } from './useArticleFilterPresentation'

describe('useArticleFilterPresentation', () => {
  afterEach(() => {
    setCurrentLocale('en')
  })

  it('summarizes active advanced filters', () => {
    const filters = createDefaultArticleFilters()
    filters.tags = ['Vue', 'Testing']
    filters.ratings = [0, 5]
    filters.createdRange = { from: '2026-05-01', to: '2026-05-31' }
    filters.readRange = { from: '2026-05-10', to: '' }

    const { presentation, app } = mountPresentation(filters)

    expect(presentation.activeFilterCount.value).toBe(4)
    expect(presentation.activeFilterSummary.value.join(' ')).toContain('Tags: 2')
    expect(presentation.activeFilterSummary.value.join(' ')).toContain('Unrated')
    expect(presentation.pageTitle.value).toBe('Filtered results')

    app.unmount()
  })

  it('prefers workspace titles for favorite, status, and single tag views', () => {
    const favorite = createDefaultArticleFilters()
    favorite.favorite = true
    const unread = createDefaultArticleFilters()
    unread.status = 'UNREAD'
    const singleTag = createDefaultArticleFilters()
    singleTag.tags = ['Vue']

    const favoriteState = mountPresentation(favorite)
    const unreadState = mountPresentation(unread)
    const tagState = mountPresentation(singleTag)

    expect(favoriteState.presentation.pageTitle.value).toBe('Favorites')
    expect(unreadState.presentation.pageTitle.value).toBe('Unread')
    expect(tagState.presentation.pageTitle.value).toBe('Vue')

    favoriteState.app.unmount()
    unreadState.app.unmount()
    tagState.app.unmount()
  })
})

function mountPresentation(filters: ReturnType<typeof createDefaultArticleFilters>) {
  let presentation!: ReturnType<typeof useArticleFilterPresentation>
  const app = createApp({
    setup() {
      presentation = useArticleFilterPresentation(ref(filters))
      return () => null
    }
  })
  app.use(i18n)
  const root = document.createElement('div')
  document.body.append(root)
  app.mount(root)
  return { presentation, app }
}
