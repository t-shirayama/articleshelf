import { describe, expect, it } from 'vitest'
import { readJwtExp } from './jwt'

describe('readJwtExp', () => {
  it('reads exp from url-safe jwt payloads', () => {
    const token = ['header', 'eyJleHAiOjEyMzQ1fQ', 'signature'].join('.')

    expect(readJwtExp(token, atob)).toBe(12345)
  })

  it('returns null for invalid tokens', () => {
    expect(readJwtExp('not-a-token', atob)).toBeNull()
    expect(readJwtExp('header.invalid.signature', atob)).toBeNull()
  })
})
