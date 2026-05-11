import { afterEach, describe, expect, it, vi } from 'vitest'
import { ProactiveRefreshTimer } from './proactiveRefreshTimer'

describe('ProactiveRefreshTimer', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('schedules refresh one minute before token expiration', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-11T00:00:00Z'))
    const refresh = vi.fn().mockResolvedValue(undefined)
    const timer = new ProactiveRefreshTimer()

    timer.schedule(tokenWithExp(Math.floor(Date.now() / 1000) + 120), refresh)

    await vi.advanceTimersByTimeAsync(59_000)
    expect(refresh).not.toHaveBeenCalled()
    await vi.advanceTimersByTimeAsync(1_000)
    expect(refresh).toHaveBeenCalledTimes(1)
  })

  it('uses a minimum delay and clears existing timers', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-11T00:00:00Z'))
    const refresh = vi.fn().mockResolvedValue(undefined)
    const timer = new ProactiveRefreshTimer()

    timer.schedule(tokenWithExp(Math.floor(Date.now() / 1000) + 10), refresh)
    timer.clear()

    await vi.advanceTimersByTimeAsync(5_000)
    expect(refresh).not.toHaveBeenCalled()
  })

  it('does not schedule refresh for tokens without exp', async () => {
    vi.useFakeTimers()
    const refresh = vi.fn().mockResolvedValue(undefined)
    const timer = new ProactiveRefreshTimer()

    timer.schedule(['header', window.btoa(JSON.stringify({ sub: 'user' })), 'signature'].join('.'), refresh)

    await vi.advanceTimersByTimeAsync(10_000)
    expect(refresh).not.toHaveBeenCalled()
  })
})

function tokenWithExp(exp: number): string {
  return ['header', window.btoa(JSON.stringify({ exp })), 'signature'].join('.')
}
