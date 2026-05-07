import { createI18n } from "vue-i18n";
import { detectInitialLocale, saveLocale, type SupportedLocale } from "./locales";
import { messages } from "./messages";

export const i18n = createI18n({
  legacy: false,
  locale: detectInitialLocale(),
  fallbackLocale: "en",
  messages,
  missingWarn: false,
  fallbackWarn: false
});

export function getCurrentLocale(): SupportedLocale {
  const locale = i18nGlobal().locale.value;
  return locale === "ja" ? "ja" : "en";
}

export function setCurrentLocale(locale: SupportedLocale): void {
  i18nGlobal().locale.value = locale;
  saveLocale(locale);
}

export function translate(key: string, params?: Record<string, unknown>): string {
  return String(i18nGlobal().t(key, params ?? {}));
}

function i18nGlobal(): {
  locale: { value: string };
  t: (key: string, params?: Record<string, unknown>) => string;
} {
  return i18n.global as unknown as {
    locale: { value: string };
    t: (key: string, params?: Record<string, unknown>) => string;
  };
}
