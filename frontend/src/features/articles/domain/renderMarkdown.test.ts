import { describe, expect, it } from 'vitest'
import { renderMarkdown } from './renderMarkdown'

describe('renderMarkdown', () => {
  it('sanitizes dangerous html and javascript links', () => {
    const html = renderMarkdown('[x](javascript:alert(1)) <img src=x onerror=alert(1)>')

    expect(html).not.toContain('href="javascript:')
    expect(html).not.toContain('<a ')
    expect(html).not.toContain('<img')
    expect(html).toContain('&lt;img src=x onerror=alert(1)&gt;')
  })

  it('keeps safe links and image attributes within the allowlist', () => {
    const html = renderMarkdown('[docs](https://example.com)\n\n![cover](https://example.com/image.png)')

    expect(html).toContain('href="https://example.com"')
    expect(html).toContain('target="_blank"')
    expect(html).toContain('rel="noopener noreferrer nofollow"')
    expect(html).toContain('src="https://example.com/image.png"')
    expect(html).toContain('loading="lazy"')
    expect(html).toContain('referrerpolicy="no-referrer"')
  })

  it('drops unsafe image schemes including data URLs', () => {
    const html = renderMarkdown('![inline](data:image/svg+xml,<svg onload=alert(1)>)')

    expect(html).not.toContain('<img')
  })

  it('does not render forbidden raw HTML tags or style attributes', () => {
    const html = renderMarkdown('<iframe src="https://example.com"></iframe><svg><script>alert(1)</script></svg><span style="color:red">text</span>')

    expect(html).not.toContain('<iframe')
    expect(html).not.toContain('<svg')
    expect(html).not.toContain('<script')
    expect(html).not.toContain('<span style=')
    expect(html).toContain('&lt;iframe')
  })

  it('keeps target blank links paired with noopener rel', () => {
    const html = renderMarkdown('[docs](https://example.com)')

    expect(html).toContain('target="_blank"')
    expect(html).toContain('rel="noopener noreferrer nofollow"')
  })

  it('escapes malformed nested HTML instead of repairing it into active nodes', () => {
    const html = renderMarkdown('<div><img src=x onerror=alert(1)</div>')

    expect(html).not.toContain('<img')
    expect(html).toContain('&lt;div&gt;')
  })

  it('renders highlighted code blocks with file names and line numbers', () => {
    const html = renderMarkdown('```ts filename="src/app.ts" showLineNumbers\nconst value = 1\nconsole.log(value)\n```')

    expect(html).toContain('markdown-code-block has-line-numbers')
    expect(html).toContain('markdown-code-file">src/app.ts')
    expect(html).toContain('markdown-code-language">ts')
    expect(html).toContain('markdown-code-line-number')
    expect(html).toContain('const')
  })

  it('escapes unknown language code blocks as inert text', () => {
    const html = renderMarkdown('```unknown file=demo.txt\n<div>safe</div>\n```')

    expect(html).toContain('language-unknown')
    expect(html).toContain('markdown-code-file">demo.txt')
    expect(html).toContain('&lt;div&gt;safe&lt;/div&gt;')
  })

  it('supports bare title metadata and empty source', () => {
    expect(renderMarkdown('')).toContain('')

    const html = renderMarkdown('```js title=example.js\n\n```')

    expect(html).toContain('markdown-code-file">example.js')
    expect(html).toContain('language-js')
    expect(html).toContain('markdown-code-line-content"> ')
  })
})
