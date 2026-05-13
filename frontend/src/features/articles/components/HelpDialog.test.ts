/* eslint-disable vue/one-component-per-file */
import { afterEach, describe, expect, it } from 'vitest'
import { createApp, defineComponent, h, type App } from 'vue'
import { i18n } from '../../../shared/i18n'
import HelpDialog from './HelpDialog.vue'

describe('HelpDialog', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('shows Chrome extension download and loading steps', () => {
    const { root, app } = mountHelpDialog()

    expect(root.textContent).toContain('Chrome 拡張機能')
    expect(root.textContent).toContain('ArticleShelf と連携して記事を保存できます')
    expect(root.textContent).toContain('読み込み手順')
    expect(root.textContent).toContain('デベロッパーモード')
    expect(root.textContent).not.toContain('ローカル開発')
    expect(root.textContent).not.toContain('本番')
    expect(root.textContent).not.toContain('vlatest')

    const downloadLink = root.querySelector<HTMLAnchorElement>('a[download]')
    expect(downloadLink?.getAttribute('href')).toBe('/downloads/articleshelf-chrome-extension-local.zip')

    app.unmount()
  })
})

function mountHelpDialog(): { root: HTMLElement, app: App<Element> } {
  const root = document.createElement('div')
  document.body.append(root)
  const globalI18n = i18n.global as unknown as { locale: { value: string } }
  globalI18n.locale.value = 'ja'

  const app = createApp({
    components: { HelpDialog },
    template: '<HelpDialog open />'
  })
  app.use(i18n)
  app.component('VDialog', passthrough('section'))
  app.component('VCard', passthrough('article'))
  app.component('VCardText', passthrough('div'))
  app.component('VBtn', defineComponent({
    inheritAttrs: false,
    props: {
      href: {
        type: String,
        default: ''
      }
    },
    emits: ['click'],
    setup(props, { attrs, emit, slots }) {
      return () => props.href
        ? h('a', { ...attrs, href: props.href, onClick: () => emit('click') }, slots.default?.())
        : h('button', { ...attrs, type: 'button', onClick: () => emit('click') }, slots.default?.())
    }
  }))
  app.mount(root)

  return { root, app }
}

function passthrough(tag: string) {
  return defineComponent({
    inheritAttrs: false,
    setup(_props, { attrs, slots }) {
      return () => h(tag, attrs, slots.default?.())
    }
  })
}
