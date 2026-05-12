import { expect, test, type Page, type TestInfo } from '@playwright/test'

const RUN_ID = Date.now().toString(36)

test('mobile drawer and bottom navigation reach primary screens', async ({ page }, testInfo) => {
  const articleTitle = `Mobile nav ${uniqueSuffix(testInfo)}`

  await loginOwner(page)
  await createArticleFromBottomNav(page, {
    url: uniqueUrl('mobile-nav', testInfo),
    title: articleTitle
  })

  await page.locator('.mobile-menu-button').click()
  await page.getByLabel('ナビゲーションメニュー').getByRole('button', { name: 'カレンダー' }).click()
  await expect(page.getByRole('heading', { name: 'カレンダー' })).toBeVisible()

  await page.locator('.mobile-menu-button').click()
  await page.getByLabel('ナビゲーションメニュー').getByRole('button', { name: 'タグ管理' }).click()
  await expect(page.getByRole('heading', { name: 'タグ管理' })).toBeVisible()

  await page.locator('.mobile-bottom-nav').getByRole('button', { name: 'すべて' }).click()
  await expect(page.getByRole('heading', { name: 'すべての記事' })).toBeVisible()
  await expect(articleCard(page, articleTitle)).toBeVisible()
})

test('mobile detail editing, unsaved confirmation, filter, and calendar day sheet work', async ({ page }, testInfo) => {
  const articleTitle = `Mobile flow ${uniqueSuffix(testInfo)}`
  const tagName = `mobile-${testInfo.workerIndex}-${testInfo.retry}`

  await loginOwner(page)
  await createArticleFromBottomNav(page, {
    url: uniqueUrl('mobile-flow', testInfo),
    title: articleTitle,
    tags: [tagName],
    notes: 'Before mobile edit'
  })

  await page.getByRole('button', { name: 'フィルタ' }).click()
  await page.getByRole('combobox', { name: '既存タグから追加' }).click()
  await page.getByRole('option', { name: tagName }).click()
  await page.getByRole('button', { name: '適用する' }).click()
  await expect(articleCard(page, articleTitle)).toBeVisible()

  await page.locator('.mobile-bottom-nav').getByRole('button', { name: 'カレンダー' }).click()
  await page.locator('.calendar-day.has-articles').first().locator('.calendar-day-header').click()
  await expect(page.getByRole('dialog')).toContainText(articleTitle)
  await page.getByRole('dialog').getByRole('button', { name: articleTitle }).click()
  await expect(page.getByRole('heading', { level: 2, name: articleTitle })).toBeVisible()

  await page.getByRole('button', { name: '編集', exact: true }).click()
  await page.getByRole('textbox', { name: 'メモ', exact: true }).fill('Edited on mobile')
  await page.getByRole('button', { name: '戻る' }).click()
  await expect(page.getByRole('dialog')).toContainText('未保存の編集があります')
  await page.getByRole('button', { name: '編集を続ける' }).click()
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.getByText('Edited on mobile')).toBeVisible()

  await page.getByRole('button', { name: '編集', exact: true }).click()
  await page.getByRole('button', { name: '削除' }).click()
  await expect(page.getByRole('dialog')).toContainText('記事を削除しますか')
  await page.getByRole('button', { name: 'キャンセル' }).click()
})

async function loginOwner(page: Page): Promise<void> {
  await page.addInitScript(() => {
    window.localStorage.setItem('articleshelf.locale', 'ja')
  })
  await page.goto('/')
  await page.getByLabel('ユーザー名').fill('owner')
  await page.getByLabel('パスワード').fill('password123')
  await page.locator('form').getByRole('button', { name: 'ログイン' }).click()
  await expect(page.getByRole('heading', { name: 'すべての記事' })).toBeVisible()
}

async function createArticleFromBottomNav(
  page: Page,
  input: { url: string, title: string, notes?: string, tags?: string[] }
): Promise<void> {
  await page.locator('.mobile-bottom-nav').getByRole('button', { name: '追加' }).click()
  await openArticleFormDetails(page)
  await page.getByRole('textbox', { name: 'URL', exact: true }).fill(input.url)
  await page.getByRole('textbox', { name: 'タイトル（任意）', exact: true }).fill(input.title)

  for (const tag of input.tags || []) {
    await page.getByRole('textbox', { name: '新しいタグ', exact: true }).fill(tag)
    await page.getByRole('textbox', { name: '新しいタグ', exact: true }).press('Enter')
  }

  if (input.notes) {
    await page.getByRole('textbox', { name: 'メモ', exact: true }).fill(input.notes)
  }

  await page.getByRole('button', { name: '保存する' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
}

async function openArticleFormDetails(page: Page): Promise<void> {
  const titleInput = page.getByRole('textbox', { name: 'タイトル（任意）', exact: true })
  if (await titleInput.isVisible().catch(() => false)) return
  await page.getByRole('button', { name: /タグ・メモ・おすすめ度を追加/ }).click()
  await expect(titleInput).toBeVisible()
}

function articleCard(page: Page, title: string) {
  return page.locator('.article-card').filter({ hasText: title })
}

function uniqueUrl(prefix: string, testInfo: TestInfo): string {
  return `https://example.com/?articleshelfMobileE2e=${prefix}-${uniqueSuffix(testInfo)}`
}

function uniqueSuffix(testInfo: TestInfo): string {
  const titleSlug = testInfo.title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')

  return `${titleSlug.slice(0, 10)}-${RUN_ID}-w${testInfo.workerIndex}-r${testInfo.retry}`
}
