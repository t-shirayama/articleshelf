import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

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

test('user separation also blocks update and delete through the API', async ({ request }) => {
  const owner = await registerByApi(request, uniqueEmail('owner'))
  const intruder = await registerByApi(request, uniqueEmail('intruder'))
  const created = await createArticleByApi(
    request,
    owner.accessToken,
    `https://example.com/?readstackE2e=${Date.now()}-${Math.random().toString(16).slice(2)}`,
    'Owner article'
  )

  const updateResponse = await request.put(apiUrl(`/api/articles/${created.id}`), {
    headers: {
      Authorization: `Bearer ${intruder.accessToken}`
    },
    data: {
      ...created.payload,
      title: 'Stolen title'
    }
  })
  expect(updateResponse.status()).toBe(404)

  const deleteResponse = await request.delete(apiUrl(`/api/articles/${created.id}`), {
    headers: {
      Authorization: `Bearer ${intruder.accessToken}`
    }
  })
  expect(deleteResponse.status()).toBe(404)

  const ownerFetch = await request.get(apiUrl(`/api/articles/${created.id}`), {
    headers: {
      Authorization: `Bearer ${owner.accessToken}`
    }
  })
  expect(ownerFetch.ok()).toBeTruthy()
  expect((await ownerFetch.json()).title).toBe('Owner article')
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

function apiUrl(path: string): string {
  return `${process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:8080'}${path}`
}

async function registerByApi(request: APIRequestContext, email: string): Promise<{ accessToken: string }> {
  const response = await request.post(apiUrl('/api/auth/register'), {
    data: {
      email,
      password: 'password123',
      displayName: 'E2E User'
    }
  })
  expect(response.ok()).toBeTruthy()
  const body = await response.json()
  return { accessToken: body.accessToken as string }
}

async function createArticleByApi(
  request: APIRequestContext,
  accessToken: string,
  url: string,
  title: string
): Promise<{ id: string, payload: Record<string, unknown> }> {
  const payload = {
    url,
    title,
    summary: '',
    status: 'UNREAD',
    favorite: false,
    rating: 0,
    notes: 'Private note',
    tags: ['private']
  }
  const response = await request.post(apiUrl('/api/articles'), {
    headers: {
      Authorization: `Bearer ${accessToken}`
    },
    data: payload
  })
  expect(response.status()).toBe(201)
  const body = await response.json()
  return { id: body.id as string, payload }
}
