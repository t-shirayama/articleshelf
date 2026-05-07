import { expect, test, type Page } from '@playwright/test'

test('user can register, create an article, logout and login again', async ({ page }) => {
  const email = uniqueEmail('reader')
  await register(page, email)

  const title = `E2E article ${Date.now()}`
  await createArticle(page, title)
  await expect(page.getByText(title)).toBeVisible()

  await page.getByRole('button', { name: /ログアウト/ }).click()
  await expect(page.getByRole('heading', { name: 'ログイン' })).toBeVisible()

  await login(page, email)
  await expect(page.getByText(title)).toBeVisible()
})

test('users cannot see each other articles', async ({ page }) => {
  const firstEmail = uniqueEmail('first')
  const secondEmail = uniqueEmail('second')
  const privateTitle = `Private article ${Date.now()}`

  await register(page, firstEmail)
  await createArticle(page, privateTitle)
  await expect(page.getByText(privateTitle)).toBeVisible()
  await page.getByRole('button', { name: /ログアウト/ }).click()

  await register(page, secondEmail)
  await expect(page.getByRole('heading', { name: 'すべての記事' })).toBeVisible()
  await expect(page.getByText(privateTitle)).toHaveCount(0)
})

async function register(page: Page, email: string): Promise<void> {
  await page.goto('/')
  await page.getByRole('button', { name: '登録', exact: true }).click()
  await page.getByLabel('メールアドレス').fill(email)
  await page.getByLabel('表示名').fill('E2E User')
  await page.getByLabel('パスワード').fill('password123')
  await page.getByRole('button', { name: '登録して始める' }).click()
  await expect(page.getByRole('heading', { name: 'すべての記事' })).toBeVisible()
}

async function login(page: Page, email: string): Promise<void> {
  await page.getByLabel('メールアドレス').fill(email)
  await page.getByLabel('パスワード').fill('password123')
  await page.locator('form').getByRole('button', { name: 'ログイン' }).click()
  await expect(page.getByRole('heading', { name: 'すべての記事' })).toBeVisible()
}

async function createArticle(page: Page, title: string): Promise<void> {
  await page.getByRole('button', { name: '最初の記事を追加' }).click()
  await page.getByLabel('URL').fill(`https://example.com/?readstackE2e=${Date.now()}-${Math.random().toString(16).slice(2)}`)
  await page.getByLabel('タイトル（任意）').fill(title)
  await page.getByLabel('メモ').fill('E2E memo')
  await page.getByRole('button', { name: '保存する' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
}

function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}@example.com`
}
