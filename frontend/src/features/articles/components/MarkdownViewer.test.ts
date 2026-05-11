import { afterEach, describe, expect, it } from 'vitest'
import { createApp, type App } from 'vue'
import MarkdownViewer from './MarkdownViewer.vue'

describe('MarkdownViewer', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('renders sanitized markdown html', () => {
    const { root, app } = mountMarkdownViewer('[docs](https://example.com) <script>alert(1)</script>')

    expect(root.querySelector('a')?.getAttribute('href')).toBe('https://example.com')
    expect(root.innerHTML).not.toContain('<script>')
    expect(root.innerHTML).toContain('&lt;script&gt;')

    app.unmount()
  })
})

function mountMarkdownViewer(source: string): { root: HTMLElement, app: App<Element> } {
  const root = document.createElement('div')
  document.body.append(root)
  const app = createApp({
    components: { MarkdownViewer },
    setup() {
      return { source }
    },
    template: '<MarkdownViewer :source="source" />'
  })
  app.mount(root)
  return { root, app }
}
