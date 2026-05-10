import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { createApp, nextTick, type App } from 'vue'
import ArticleCard from './ArticleCard.vue'
import { i18n } from '../../../shared/i18n'
import type { Article } from '../types'

describe('ArticleCard', () => {
  beforeAll(() => {
    class ResizeObserverStub {
      observe(): void {}
      unobserve(): void {}
      disconnect(): void {}
    }

    vi.stubGlobal('ResizeObserver', ResizeObserverStub)
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('separates the open-detail control from card action buttons', async () => {
    const onOpen = vi.fn()
    const onStatus = vi.fn()
    const onFavorite = vi.fn()
    const onDelete = vi.fn()
    const { root, app } = mountArticleCard({
      onOpen,
      onStatus,
      onFavorite,
      onDelete
    })

    await nextTick()

    const card = root.querySelector('.article-card')
    const openButton = root.querySelector<HTMLButtonElement>('.article-card-open')
    expect(card?.getAttribute('role')).toBeNull()
    expect(card?.getAttribute('tabindex')).toBeNull()
    expect(openButton?.tagName).toBe('BUTTON')
    expect(openButton?.querySelector('.status-toggle-button')).toBeNull()
    expect(openButton?.querySelector('.card-favorite-button')).toBeNull()
    expect(openButton?.querySelector('.card-delete-button')).toBeNull()

    openButton?.click()
    root.querySelector<HTMLButtonElement>('.status-toggle-button')?.click()
    root.querySelector<HTMLButtonElement>('.card-favorite-button')?.click()
    root.querySelector<HTMLButtonElement>('.card-delete-button')?.click()

    expect(onOpen).toHaveBeenCalledTimes(1)
    expect(onStatus).toHaveBeenCalledTimes(1)
    expect(onFavorite).toHaveBeenCalledTimes(1)
    expect(onDelete).toHaveBeenCalledTimes(1)

    app.unmount()
  })
})

function mountArticleCard(handlers: {
  onOpen: () => void
  onStatus: () => void
  onFavorite: () => void
  onDelete: () => void
}): { root: HTMLElement, app: App<Element> } {
  const root = document.createElement('div')
  document.body.append(root)

  const app = createApp({
    components: { ArticleCard },
    setup() {
      return {
        article: createArticle(),
        ...handlers
      }
    },
    template: `
      <ArticleCard
        :article="article"
        @click="onOpen"
        @toggle-status="onStatus"
        @toggle-favorite="onFavorite"
        @delete="onDelete"
      />
    `
  })

  app.use(i18n)
  app.component('VCard', {
    inheritAttrs: false,
    template: '<div v-bind="$attrs"><slot /></div>'
  })
  app.component('VBtn', {
    inheritAttrs: false,
    template: '<button v-bind="$attrs"><slot /></button>'
  })
  app.component('VChip', {
    inheritAttrs: false,
    template: '<span v-bind="$attrs"><slot /></span>'
  })
  app.mount(root)

  return { root, app }
}

function createArticle(): Article {
  return {
    id: 'article-1',
    url: 'https://example.com/articles/vue-accessibility',
    title: 'Vue accessibility patterns',
    summary: 'Keep controls independent for keyboard users.',
    thumbnailUrl: '',
    status: 'UNREAD',
    readDate: null,
    favorite: false,
    rating: 4,
    notes: '',
    tags: [{ name: 'Vue' }],
    createdAt: '2026-05-10T00:00:00Z',
    updatedAt: '2026-05-10T00:00:00Z'
  }
}
