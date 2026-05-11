import { afterEach, describe, expect, it } from 'vitest'
import { createApp, nextTick, ref, type App } from 'vue'
import { i18n } from '../i18n'
import StarRating from './StarRating.vue'

describe('StarRating', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('emits selected ratings and previews hovered stars', async () => {
    const selected = ref(2)
    const { root, app } = mountStarRating(selected)
    const buttons = root.querySelectorAll<HTMLButtonElement>('.star-rating-button')

    buttons[3].dispatchEvent(new MouseEvent('mouseenter'))
    await nextTick()
    expect(buttons[3].classList.contains('is-preview')).toBe(true)

    buttons[4].click()
    await nextTick()
    expect(selected.value).toBe(5)

    root.querySelector('.star-rating')?.dispatchEvent(new MouseEvent('mouseleave'))
    await nextTick()
    expect(buttons[4].classList.contains('is-preview')).toBe(false)

    app.unmount()
  })

  it('does not emit in readonly mode', async () => {
    const selected = ref(3)
    const { root, app } = mountStarRating(selected, true)

    root.querySelector<HTMLButtonElement>('.star-rating-button')?.click()
    await nextTick()

    expect(selected.value).toBe(3)
    expect(root.querySelector<HTMLButtonElement>('.star-rating-button')?.disabled).toBe(true)

    app.unmount()
  })
})

function mountStarRating(selected: { value: number }, readonly = false): { root: HTMLElement, app: App<Element> } {
  const root = document.createElement('div')
  document.body.append(root)
  const app = createApp({
    components: { StarRating },
    setup() {
      return { selected, readonly }
    },
    template: '<StarRating v-model="selected" :readonly="readonly" />'
  })
  app.use(i18n)
  app.mount(root)
  return { root, app }
}
