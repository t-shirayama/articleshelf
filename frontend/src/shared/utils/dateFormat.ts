import { getCurrentLocale, translate } from "../i18n";

export function formatDate(value?: string | null, fallback = translate("common.unset")): string {
  if (!value) return fallback

  const parsed = parseDate(value)
  if (!parsed) return value
  return createDateFormatter().format(parsed)
}

export function formatDateTime(value?: string | null, fallback = translate("common.unset")): string {
  if (!value) return fallback

  const parsed = parseDate(value)
  if (!parsed) return value
  return createDateTimeFormatter().format(parsed)
}

function createDateFormatter(): Intl.DateTimeFormat {
  return new Intl.DateTimeFormat(currentIntlLocale(), {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

function createDateTimeFormatter(): Intl.DateTimeFormat {
  return new Intl.DateTimeFormat(currentIntlLocale(), {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function currentIntlLocale(): string {
  return getCurrentLocale() === 'ja' ? 'ja-JP' : 'en-US'
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
