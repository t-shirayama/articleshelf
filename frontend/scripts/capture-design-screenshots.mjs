import { mkdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const frontendDir = path.resolve(__dirname, "..");
const repoRoot = path.resolve(frontendDir, "..");
const outputDir = path.join(repoRoot, "docs", "designs");
const baseUrl =
  process.env.READSTACK_SCREENSHOT_BASE_URL || "http://localhost:5173";
const desktopViewport = { width: 1920, height: 1080 };
const mobileViewport = { width: 430, height: 932 };

async function waitForArticleList(page) {
  await page.goto(baseUrl, { waitUntil: "networkidle" });
  await page.waitForSelector(".article-list, .empty-state", { timeout: 30000 });
  await page.waitForTimeout(1200);
}

async function captureDesktopList(page) {
  await page.setViewportSize(desktopViewport);
  await waitForArticleList(page);
  await page.screenshot({
    path: path.join(outputDir, "desktop_article_list.png"),
  });
}

async function captureDesktopDetail(page) {
  await page.setViewportSize(desktopViewport);
  await waitForArticleList(page);
  const firstCard = page.locator(".article-card").first();

  if ((await firstCard.count()) === 0) {
    throw new Error("記事一覧が空のため、詳細画面のスクリーンショットを撮影できません。");
  }

  await firstCard.click();
  await page.waitForSelector(".detail-page", { timeout: 30000 });
  await page.waitForTimeout(800);
  await page.screenshot({
    path: path.join(outputDir, "desktop_article_detail_light.png"),
  });
}

async function captureAddModal(page) {
  await page.setViewportSize(desktopViewport);
  await waitForArticleList(page);
  await page.getByRole("button", { name: "記事を追加" }).click();
  await page.waitForSelector(".article-modal", { timeout: 30000 });
  await page.waitForTimeout(500);
  await page.screenshot({
    path: path.join(outputDir, "add_article_modal.png"),
  });
}

async function captureMobileList(page) {
  await page.setViewportSize(mobileViewport);
  await waitForArticleList(page);
  await page.screenshot({
    path: path.join(outputDir, "mobile_article_list.png"),
    fullPage: true,
  });
}

async function main() {
  await mkdir(outputDir, { recursive: true });

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    locale: "ja-JP",
    viewport: desktopViewport,
  });
  const page = await context.newPage();

  try {
    await captureDesktopList(page);
    await captureDesktopDetail(page);
    await captureAddModal(page);
    await captureMobileList(page);
    console.log(`Captured design screenshots in ${outputDir}`);
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
