import { describe, expect, it } from 'vitest'
import headers from '../../../public/_headers?raw'

describe('frontend CSP headers', () => {
  it('narrows inline style allowance to style elements only', () => {
    expect(headers).toContain("style-src 'self';")
    expect(headers).toContain("style-src-elem 'self' 'unsafe-inline';")
    expect(headers).toContain("style-src-attr 'none';")
    expect(headers).not.toContain("style-src 'self' 'unsafe-inline'")
  })
})
