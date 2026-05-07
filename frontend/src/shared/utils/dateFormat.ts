const dateFormatter = new Intl.DateTimeFormat('ja-JP', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit'
})

const dateTimeFormatter = new Intl.DateTimeFormat('ja-JP', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit'
})

export function formatDate(value?: string | null, fallback = '未設定'): string {
  if (!value) return fallback

  const parsed = parseDate(value)
  if (!parsed) return value
  return dateFormatter.format(parsed)
}

export function formatDateTime(value?: string | null, fallback = '未設定'): string {
  if (!value) return fallback

  const parsed = parseDate(value)
  if (!parsed) return value
  return dateTimeFormatter.format(parsed)
}

function parseDate(value: string): Date | null {
  const dateOnly = value.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (dateOnly) {
    const [, year, month, day] = dateOnly
    return new Date(Number(year), Number(month) - 1, Number(day))
  }

  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}
