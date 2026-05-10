import { readJwtExp } from '../../../shared/auth/jwt'

export class ProactiveRefreshTimer {
  private timer: ReturnType<typeof window.setTimeout> | undefined

  schedule(token: string, refresh: () => Promise<unknown>): void {
    this.clear()
    const exp = readJwtExp(token)
    if (!exp) return
    const refreshAt = exp * 1000 - Date.now() - 60_000
    this.timer = window.setTimeout(() => {
      void refresh()
    }, Math.max(refreshAt, 5_000))
  }

  clear(): void {
    if (this.timer) window.clearTimeout(this.timer)
    this.timer = undefined
  }
}
