export function readJwtExp(token: string, decodeBase64: (value: string) => string = globalThis.atob): number | null {
  const payload = token.split('.')[1]
  if (!payload || typeof decodeBase64 !== 'function') return null
  try {
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const normalized = base64.padEnd(base64.length + ((4 - base64.length % 4) % 4), '=')
    const decoded = JSON.parse(decodeBase64(normalized))
    return typeof decoded.exp === 'number' ? decoded.exp : null
  } catch {
    return null
  }
}
