import { afterEach, describe, expect, it, vi } from "vitest";
import { detectInitialLocale, LOCALE_STORAGE_KEY, normalizeLocale } from "./locales";

describe("locale detection", () => {
  afterEach(() => {
    window.localStorage.clear();
    vi.unstubAllGlobals();
  });

  it("normalizes supported browser locales", () => {
    expect(normalizeLocale("ja-JP")).toBe("ja");
    expect(normalizeLocale("en-US")).toBe("en");
    expect(normalizeLocale("fr-FR")).toBeNull();
  });

  it("uses localStorage before browser languages", () => {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, "ja");
    vi.stubGlobal("navigator", { languages: ["en-US"], language: "en-US" });

    expect(detectInitialLocale()).toBe("ja");
  });

  it("uses supported browser language when there is no saved locale", () => {
    vi.stubGlobal("navigator", { languages: ["ja-JP"], language: "ja-JP" });

    expect(detectInitialLocale()).toBe("ja");
  });

  it("falls back to English for unsupported languages", () => {
    vi.stubGlobal("navigator", { languages: ["fr-FR"], language: "fr-FR" });

    expect(detectInitialLocale()).toBe("en");
  });
});
