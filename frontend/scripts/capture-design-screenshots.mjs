import { mkdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium, request } from "playwright";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const frontendDir = path.resolve(__dirname, "..");
const repoRoot = path.resolve(frontendDir, "..");
const outputDir = path.join(repoRoot, "docs", "designs", "screenshots");
const baseUrl =
  process.env.ARTICLESHELF_SCREENSHOT_BASE_URL || "http://localhost:5173";
const apiBaseUrl =
  process.env.ARTICLESHELF_SCREENSHOT_API_BASE_URL ||
  process.env.E2E_API_BASE_URL ||
  "http://127.0.0.1:8080";
const desktopViewport = { width: 1920, height: 1080 };
const mobileViewport = { width: 430, height: 932 };
const captureId = Date.now().toString(36);
const captureTarget = process.env.ARTICLESHELF_SCREENSHOT_TARGET || "all";
const captureUsername =
  process.env.ARTICLESHELF_CAPTURE_USERNAME || "owner";
const capturePassword =
  process.env.ARTICLESHELF_CAPTURE_PASSWORD || "password123";
const standaloneTagName = "未使用";

const captureArticles = [
  {
    url: `https://example.com/?articleshelfCapture=vue-${captureId}`,
    title: "Vue で育てる記事棚の設計メモ",
    summary: "一覧、詳細、タグをつなげる ArticleShelf の設計観点。",
    status: "UNREAD",
    favorite: true,
    rating: 5,
    notes: "カードの密度と読み返しやすさを確認するためのキャプチャ用メモ。",
    tags: ["Vue", "設計"],
  },
  {
    url: `https://example.com/?articleshelfCapture=spring-${captureId}`,
    title: "Spring Boot API エラー設計チェック",
    summary: "検証エラーと検索 API の回帰を確認する記事。",
    status: "READ",
    readDate: "2026-05-08",
    favorite: false,
    rating: 4,
    notes: "バックエンドのエラー表示と日付表示の見え方を揃える。",
    tags: ["Spring", "API"],
  },
  {
    url: `https://example.com/?articleshelfCapture=tags-${captureId}`,
    title: "タグ管理 UI の使い勝手レビュー",
    summary: "検索、並び替え、統合、削除確認のデザイン確認。",
    status: "UNREAD",
    favorite: false,
    rating: 3,
    notes: "タグ管理画面とモーダルのスクリーンショットに使う。",
    tags: ["UI", "タグ"],
  },
];

async function createCaptureData() {
  const api = await request.newContext({
    baseURL: apiBaseUrl,
    extraHTTPHeaders: {
      "Accept-Language": "ja",
    },
  });

  const loginResponse = await api.post("/api/auth/login", {
    data: {
      username: captureUsername,
      password: capturePassword,
    },
  });

  let auth;
  if (loginResponse.ok()) {
    auth = await loginResponse.json();
  } else if (loginResponse.status() === 401) {
    const registerResponse = await api.post("/api/auth/register", {
      data: {
        username: captureUsername,
        password: capturePassword,
        displayName: "Capture User",
      },
    });

    if (!registerResponse.ok()) {
      throw new Error(
        `キャプチャ用ユーザーの作成に失敗しました: ${registerResponse.status()} ${await registerResponse.text()}`,
      );
    }

    auth = await registerResponse.json();
  } else {
    throw new Error(
      `キャプチャ用ログインに失敗しました: ${loginResponse.status()} ${await loginResponse.text()}`,
    );
  }

  const accessToken = auth.accessToken;
  const authHeaders = { Authorization: `Bearer ${accessToken}` };

  for (const article of captureArticles) {
    const response = await api.post("/api/articles", {
      headers: authHeaders,
      data: article,
    });
    if (!response.ok() && response.status() !== 409) {
      throw new Error(
        `キャプチャ用記事の作成に失敗しました: ${response.status()} ${await response.text()}`,
      );
    }
  }

  const standaloneTagResponse = await api.post("/api/tags", {
    headers: authHeaders,
    data: { name: standaloneTagName },
  });
  if (!standaloneTagResponse.ok() && standaloneTagResponse.status() !== 409) {
    throw new Error(
      `キャプチャ用タグの作成に失敗しました: ${standaloneTagResponse.status()} ${await standaloneTagResponse.text()}`,
    );
  }

  await api.dispose();
  return { username: captureUsername };
}

async function createContext(browser, storageState) {
  const context = await browser.newContext({
    colorScheme: "light",
    deviceScaleFactor: 1,
    locale: "ja-JP",
    reducedMotion: "reduce",
    storageState,
    viewport: desktopViewport,
  });

  await context.addInitScript(() => {
    window.localStorage.setItem("articleshelf.locale", "ja");
  });

  return context;
}

async function loginCaptureUser(page, username) {
  await page.goto(baseUrl, { waitUntil: "domcontentloaded" });
  await page.waitForSelector("form", { timeout: 30000 });
  await page.getByLabel("ユーザー名").fill(username);
  await page.getByLabel("パスワード").fill(capturePassword);
  await page.locator("form").getByRole("button", { name: "ログイン" }).click();
  await page.waitForSelector(".article-list", { timeout: 30000 });
  await page.waitForSelector(".article-card", { timeout: 30000 });
}

async function openArticleList(page) {
  await page.setViewportSize(desktopViewport);
  await page.goto(baseUrl, { waitUntil: "domcontentloaded" });
  await page.waitForSelector(".article-list", { timeout: 30000 });
  await page.waitForSelector(".article-card", { timeout: 30000 });
  await page.waitForTimeout(700);
}

async function saveScreenshot(page, filename, options = {}) {
  await page.screenshot({
    animations: "disabled",
    path: path.join(outputDir, filename),
    ...options,
  });
}

async function captureAuthLogin(browser) {
  const context = await createContext(browser);
  const page = await context.newPage();
  await page.goto(baseUrl, { waitUntil: "domcontentloaded" });
  await page.waitForSelector("form", { timeout: 30000 });
  await page.waitForTimeout(500);
  await saveScreenshot(page, "auth_login.png");
  await context.close();
}

async function captureDesktopList(page) {
  await openArticleList(page);
  await saveScreenshot(page, "desktop_article_list.png");
}

async function openFirstArticleDetail(page) {
  await openArticleList(page);
  await page.locator(".article-card").first().click();
  await page.waitForSelector(".detail-page", { timeout: 30000 });
  await page.waitForTimeout(700);
}

async function captureDesktopDetailView(page) {
  await openFirstArticleDetail(page);
  await saveScreenshot(page, "desktop_article_detail_view.png");
}

async function captureDesktopDetailEdit(page) {
  await openFirstArticleDetail(page);
  await page.getByRole("button", { name: "編集", exact: true }).click();
  await page.waitForSelector(".detail-page.is-editing", { timeout: 30000 });
  await page.waitForTimeout(700);
  await saveScreenshot(page, "desktop_article_detail_edit.png");
}

async function captureCalendarView(page) {
  await openArticleList(page);
  await page.getByRole("button", { name: "カレンダー" }).click();
  await page.waitForSelector(".calendar-view", { timeout: 30000 });
  await page.waitForTimeout(700);
  await saveScreenshot(page, "calendar_view.png");
}

async function captureTagManagement(page) {
  await openArticleList(page);
  await page.getByRole("button", { name: "タグ管理", exact: true }).click();
  await page.waitForSelector(".tag-management-view", { timeout: 30000 });
  await page.waitForTimeout(700);
  await saveScreenshot(page, "tag_management.png");
}

async function captureAddModal(page) {
  await openArticleList(page);
  await page.getByRole("button", { name: "記事を追加", exact: true }).click();
  await page.waitForSelector(".article-modal", { timeout: 30000 });
  await page.waitForTimeout(500);
  await saveScreenshot(page, "add_article_modal.png");
}

async function captureFilterDialog(page) {
  await openArticleList(page);
  await page.getByRole("button", { name: "フィルタ" }).click();
  await page.waitForSelector(".filter-dialog", { timeout: 30000 });
  await page.waitForTimeout(500);
  await saveScreenshot(page, "filter_dialog.png");
}

async function captureTagAddDialog(page) {
  await openArticleList(page);
  await page.getByRole("button", { name: "タグ管理", exact: true }).click();
  await page.waitForSelector(".tag-management-view", { timeout: 30000 });
  await page.getByRole("button", { name: "タグを追加" }).click();
  await page.waitForSelector(".tag-management-dialog", { timeout: 30000 });
  await page.waitForTimeout(500);
  await saveScreenshot(page, "tag_add_dialog.png");
}

async function captureTagMergeDialog(page) {
  await openArticleList(page);
  await page.getByRole("button", { name: "タグ管理", exact: true }).click();
  await page.waitForSelector(".tag-management-view", { timeout: 30000 });
  await page
    .locator(".tag-management-row")
    .filter({ hasText: "Vue" })
    .getByRole("button", { name: "統合" })
    .click();
  await page.waitForSelector(".tag-management-dialog", { timeout: 30000 });
  await page.waitForTimeout(500);
  await saveScreenshot(page, "tag_merge_dialog.png");
}

async function captureTagDeleteDialog(page) {
  await openArticleList(page);
  await page.getByRole("button", { name: "タグ管理", exact: true }).click();
  await page.waitForSelector(".tag-management-view", { timeout: 30000 });
  await page
    .locator(".tag-management-row")
    .filter({ hasText: standaloneTagName })
    .getByRole("button", { name: "削除" })
    .click();
  await page.waitForSelector(".tag-management-dialog", { timeout: 30000 });
  await page.waitForTimeout(500);
  await saveScreenshot(page, "tag_delete_dialog.png");
}

async function captureDeleteArticleDialog(page) {
  await openArticleList(page);
  await page.locator(".article-card").first().click();
  await page.waitForSelector(".detail-page", { timeout: 30000 });
  await page.getByRole("button", { name: "削除" }).click();
  await page.waitForSelector(".delete-confirm-dialog", { timeout: 30000 });
  await page.waitForTimeout(500);
  await saveScreenshot(page, "delete_article_dialog.png");
}

async function captureAccountSettingsDialog(page) {
  await openArticleList(page);
  await page.getByRole("button", { name: "アカウント", exact: true }).click();
  await page.waitForSelector(".account-settings-dialog", { timeout: 30000 });
  await page.waitForTimeout(500);
  await saveScreenshot(page, "account_settings_dialog.png");
}

async function captureMobileList(page) {
  await page.setViewportSize(mobileViewport);
  await page.goto(baseUrl, { waitUntil: "domcontentloaded" });
  await page.waitForSelector(".article-list", { timeout: 30000 });
  await page.waitForTimeout(700);
  await saveScreenshot(page, "mobile_article_list.png", { fullPage: true });
}

async function main() {
  await mkdir(outputDir, { recursive: true });

  const browser = await chromium.launch({
    args: ["--host-resolver-rules=MAP localhost 127.0.0.1"],
    headless: true,
  });

  try {
    const captureData = await createCaptureData();
    const context = await createContext(browser);
    const page = await context.newPage();
    await loginCaptureUser(page, captureData.username);

    if (captureTarget === "account-settings-dialog") {
      await captureAccountSettingsDialog(page);
      await context.close();
      console.log(`Captured account settings screenshot in ${outputDir}`);
      return;
    }

    if (captureTarget === "add-article-modal") {
      await captureAddModal(page);
      await context.close();
      console.log(`Captured add article modal screenshot in ${outputDir}`);
      return;
    }

    await captureAuthLogin(browser);
    await captureDesktopList(page);
    await captureDesktopDetailView(page);
    await captureDesktopDetailEdit(page);
    await captureCalendarView(page);
    await captureTagManagement(page);
    await captureAddModal(page);
    await captureFilterDialog(page);
    await captureTagAddDialog(page);
    await captureTagMergeDialog(page);
    await captureTagDeleteDialog(page);
    await captureDeleteArticleDialog(page);
    await captureAccountSettingsDialog(page);
    await captureMobileList(page);

    await context.close();
    console.log(`Captured design screenshots in ${outputDir}`);
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
