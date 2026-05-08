import { afterEach, describe, expect, it } from "vitest";
import { LOCALE_STORAGE_KEY } from "../i18n/locales";
import { setCurrentLocale } from "../i18n";
import { formatDate } from "./dateFormat";

describe("date formatting", () => {
  afterEach(() => {
    window.localStorage.removeItem(LOCALE_STORAGE_KEY);
    setCurrentLocale("en");
  });

  it("formats dates with Japanese locale", () => {
    setCurrentLocale("ja");

    expect(formatDate("2026-05-07")).toBe("2026/05/07");
  });

  it("formats dates with English locale", () => {
    setCurrentLocale("en");

    expect(formatDate("2026-05-07")).toBe("05/07/2026");
  });
});
