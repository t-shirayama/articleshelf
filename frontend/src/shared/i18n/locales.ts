export type SupportedLocale = "ja" | "en";

export const DEFAULT_LOCALE: SupportedLocale = "en";
export const LOCALE_STORAGE_KEY = "articleshelf.locale";

export function normalizeLocale(value?: string | null): SupportedLocale | null {
  if (!value) return null;
  const normalized = value.toLowerCase();
  if (normalized === "ja" || normalized.startsWith("ja-")) return "ja";
  if (normalized === "en" || normalized.startsWith("en-")) return "en";
  return null;
}

export function detectInitialLocale(): SupportedLocale {
  const storedLocale = readStoredLocale();
  if (storedLocale) return storedLocale;

  const browserLocales = readBrowserLocales();
  for (const locale of browserLocales) {
    const supportedLocale = normalizeLocale(locale);
    if (supportedLocale) return supportedLocale;
  }

  return DEFAULT_LOCALE;
}

export function saveLocale(locale: SupportedLocale): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(LOCALE_STORAGE_KEY, locale);
}

function readStoredLocale(): SupportedLocale | null {
  if (typeof window === "undefined") return null;
  return normalizeLocale(window.localStorage.getItem(LOCALE_STORAGE_KEY));
}

function readBrowserLocales(): string[] {
  if (typeof navigator === "undefined") return [];
  const languages = navigator.languages?.length ? navigator.languages : [];
  return [...languages, navigator.language].filter(Boolean);
}
