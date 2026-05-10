import { describe, expect, it } from 'vitest'
import { readDateForStatus } from './articleStatus'

describe('articleStatus', () => {
  it('sets today when changing to read without an existing read date', () => {
    expect(readDateForStatus('READ', null, '2026-05-10')).toBe('2026-05-10')
  })

  it('keeps an existing read date when changing to read', () => {
    expect(readDateForStatus('READ', '2026-05-07', '2026-05-10')).toBe('2026-05-07')
  })

  it('clears read date when changing to unread', () => {
    expect(readDateForStatus('UNREAD', '2026-05-07', '2026-05-10')).toBeNull()
  })
})
