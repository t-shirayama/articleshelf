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
})
