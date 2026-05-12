/* eslint-disable vue/one-component-per-file */
import { afterEach, describe, expect, it } from 'vitest'
import { createApp, type App } from 'vue'
import { i18n } from '../../../shared/i18n'
import MotivationCard from './MotivationCard.vue'
import type { MotivationCardData } from '../types'

describe('MotivationCard', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('renders card text and theme variables', () => {
    const { root, app } = mountMotivationCard({
      id: 1,
      title: 'Read small',
      note: 'One article is enough.',
      illustration: 'spark',
      paletteClass: 'boost-palette-3'
    })

    const card = root.querySelector<HTMLElement>('.learning-boost-card')
    expect(card?.textContent).toContain('Read small')
    expect(card?.textContent).toContain('One article is enough.')
    expect(card?.classList.contains('boost-palette-3')).toBe(true)

    app.unmount()
  })
})

function mountMotivationCard(card: MotivationCardData): { root: HTMLElement, app: App<Element> } {
  const root = document.createElement('div')
  document.body.append(root)
  const app = createApp({
    components: { MotivationCard },
    setup() {
      return { card }
    },
    template: '<MotivationCard :card="card" />'
  })
  app.use(i18n)
  app.component('MotivationIllustration', {
    props: {
      card: {
        type: Object,
        required: true
      }
    },
    template: '<span class="illustration">{{ card.illustration }}</span>'
  })
  app.mount(root)
  return { root, app }
}
