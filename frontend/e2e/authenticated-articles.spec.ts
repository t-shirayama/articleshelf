import { expect, test, type APIRequestContext, type Page, type TestInfo } from '@playwright/test'

const RUN_ID = Date.now().toString(36)

test('user can register, create an article, logout and login again', async ({ page }, testInfo) => {
  const email = uniqueEmail('reader', testInfo)
  const articleTitle = `E2E article ${uniqueSuffix(testInfo)}`

  await register(page, email)
  await createArticle(page, {
    url: uniqueUrl('register-login', testInfo),
    title: articleTitle,
    notes: 'E2E memo'
  })

  await expect(articleCard(page, articleTitle)).toBeVisible()

  await page.getByRole('button', { name: /ログアウト/ }).click()
  await expect(page.getByRole('heading', { name: 'ログイン' })).toBeVisible()

  await login(page, email)
  await expect(articleCard(page, articleTitle)).toBeVisible()
})

test('duplicate article url shows an error and can open the existing article', async ({ page }, testInfo) => {
  const email = uniqueEmail('duplicate', testInfo)
  const sharedUrl = uniqueUrl('duplicate', testInfo)
  const originalTitle = `Original ${uniqueSuffix(testInfo)}`

  await register(page, email)
  await createArticle(page, {
    url: sharedUrl,
    title: originalTitle
  })

  await openArticleModal(page)
  await page.getByRole('textbox', { name: 'URL', exact: true }).fill(sharedUrl)
  await page.getByRole('textbox', { name: 'タイトル（任意）', exact: true }).fill(`Duplicate ${uniqueSuffix(testInfo)}`)
  await page.getByRole('button', { name: '保存する' }).click()

  await expect(page.getByRole('alert')).toContainText('このURLはすでに登録されています')
  await page.getByRole('button', { name: '登録済みの記事を開く' }).click()

  await expect(page.getByRole('heading', { level: 2, name: originalTitle })).toBeVisible()
  await expect(page.getByRole('dialog')).toHaveCount(0)
})

test('user can edit article details and persist notes tags and rating', async ({ page }, testInfo) => {
  const email = uniqueEmail('detail-edit', testInfo)
  const articleTitle = `Editable ${uniqueSuffix(testInfo)}`

  await register(page, email)
  await createArticle(page, {
    url: uniqueUrl('detail-edit', testInfo),
    title: articleTitle,
    notes: 'Before edit'
  })

  await openArticleFromList(page, articleTitle)
  await page.getByRole('button', { name: '編集' }).click()
  await page.getByRole('textbox', { name: 'メモ', exact: true }).fill('Edited memo with details')
  await page.getByRole('button', { name: '記事の詳細' }).click()
  await page.getByRole('textbox', { name: 'タイトル', exact: true }).fill(`${articleTitle} updated`)
  await page.getByRole('textbox', { name: '新しいタグ', exact: true }).fill('Vue')
  await page.getByRole('textbox', { name: '新しいタグ', exact: true }).press('Enter')
  await page.getByRole('button', { name: 'おすすめ度 4 を選択' }).click()
  await page.getByRole('textbox', { name: '概要', exact: true }).fill('Edited summary')
  await page.getByRole('button', { name: '保存' }).click()

  await expect(page.getByRole('heading', { level: 2, name: `${articleTitle} updated` })).toBeVisible()
  await expect(page.getByText('Edited memo with details')).toBeVisible()
  await expect(page.getByText('Vue')).toBeVisible()
  await expect(page.getByLabel('おすすめ度 4 / 5')).toBeVisible()
})

test('user can delete an article from the detail view', async ({ page }, testInfo) => {
  const email = uniqueEmail('detail-delete', testInfo)
  const articleTitle = `Delete me ${uniqueSuffix(testInfo)}`

  await register(page, email)
  await createArticle(page, {
    url: uniqueUrl('detail-delete', testInfo),
    title: articleTitle
  })

  await openArticleFromList(page, articleTitle)
  await page.getByRole('button', { name: '削除' }).click()
  await page.getByRole('button', { name: '削除する' }).click()

  await expect(page.getByRole('heading', { name: 'すべての記事' })).toBeVisible()
  await expect(articleCard(page, articleTitle)).toHaveCount(0)
})

test('user can toggle article status between unread and read from the list', async ({ page }, testInfo) => {
  const email = uniqueEmail('status-toggle', testInfo)
  const articleTitle = `Toggle ${uniqueSuffix(testInfo)}`

  await register(page, email)
  await createArticle(page, {
    url: uniqueUrl('status-toggle', testInfo),
    title: articleTitle
  })

  await articleCard(page, articleTitle).locator('.status-toggle-button').click()
  await expect(page.getByText('既読にしました')).toBeVisible()
  await expect(articleCard(page, articleTitle)).toContainText('既読')

  await articleCard(page, articleTitle).locator('.status-toggle-button').click()
  await expect(page.getByText('未読に戻しました')).toBeVisible()
  await expect(articleCard(page, articleTitle)).toContainText('未読')
})

test('user can filter articles by search, tag and rating', async ({ page }, testInfo) => {
  const email = uniqueEmail('filter', testInfo)
  const matchingTitle = `Vue guide ${uniqueSuffix(testInfo)}`
  const nonMatchingTitle = `Java guide ${uniqueSuffix(testInfo)}`

  await register(page, email)
  await createArticle(page, {
    url: uniqueUrl('filter-match', testInfo),
    title: matchingTitle,
    notes: 'pinia router tips',
    tags: ['Vue'],
    rating: 4
  })
  await createArticle(page, {
    url: uniqueUrl('filter-other', testInfo),
    title: nonMatchingTitle,
    notes: 'spring security tips',
    tags: ['Java'],
    rating: 2
  })

  await page.getByPlaceholder('タイトル・URL・メモで検索').fill('pinia')
  await page.getByRole('button', { name: 'フィルタ' }).click()
  const filterDialog = page.getByRole('dialog')
  await filterDialog.getByRole('combobox', { name: '既存タグから追加' }).click()
  await page.getByRole('option', { name: 'Vue' }).click()
  await filterDialog.getByRole('button', { name: '4 / 5', exact: true }).click()
  await filterDialog.getByRole('button', { name: '適用する' }).click()

  await expect(articleCard(page, matchingTitle)).toBeVisible()
  await expect(articleCard(page, nonMatchingTitle)).toHaveCount(0)
  await expect(page.getByText('適用中')).toBeVisible()
})

test('user can search tags and open articles from tag management', async ({ page }, testInfo) => {
  const email = uniqueEmail('tag-management', testInfo)
  const matchingTitle = `Tagged AI ${uniqueSuffix(testInfo)}`
  const nonMatchingTitle = `Tagged Ops ${uniqueSuffix(testInfo)}`

  await register(page, email)
  await createArticle(page, {
    url: uniqueUrl('tag-management-ai', testInfo),
    title: matchingTitle,
    tags: ['AI']
  })
  await createArticle(page, {
    url: uniqueUrl('tag-management-ops', testInfo),
    title: nonMatchingTitle,
    tags: ['Ops']
  })

  await page.getByRole('button', { name: 'タグ管理' }).click()
  await expect(page.getByRole('heading', { name: /タグ管理/ })).toBeVisible()
  await expect(page.getByText('2件')).toBeVisible()

  await page.getByPlaceholder('タグ名で検索').fill('AI')
  const aiRow = page.locator('.tag-management-row').filter({ hasText: 'AI' })
  await expect(aiRow).toBeVisible()
  await expect(page.locator('.tag-management-row').filter({ hasText: 'Ops' })).toHaveCount(0)
  await expect(aiRow.getByRole('button', { name: '削除' })).toBeDisabled()

  await aiRow.getByRole('button', { name: 'AI の記事一覧を開く' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'AI' })).toBeVisible()
  await expect(articleCard(page, matchingTitle)).toBeVisible()
  await expect(articleCard(page, nonMatchingTitle)).toHaveCount(0)
})

test('user can create a standalone tag from tag management', async ({ page }, testInfo) => {
  const email = uniqueEmail('tag-create', testInfo)
  const tagName = `Idea-${uniqueSuffix(testInfo)}`

  await register(page, email)
  await page.getByRole('button', { name: 'タグ管理' }).click()
  await expect(page.getByRole('heading', { name: /タグ管理/ })).toBeVisible()
  await page.getByRole('button', { name: '最初のタグを追加' }).click()

  const dialog = page.getByRole('dialog')
  await dialog.getByRole('textbox', { name: 'タグ名' }).fill(tagName)
  await dialog.getByRole('button', { name: '追加する' }).click()

  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.locator('.tag-management-row').filter({ hasText: tagName })).toBeVisible()
})

test('users cannot see each other articles', async ({ page }, testInfo) => {
  const firstEmail = uniqueEmail('first', testInfo)
  const secondEmail = uniqueEmail('second', testInfo)
  const privateTitle = `Private article ${uniqueSuffix(testInfo)}`

  await register(page, firstEmail)
  await createArticle(page, {
    url: uniqueUrl('private-first', testInfo),
    title: privateTitle
  })
  await expect(articleCard(page, privateTitle)).toBeVisible()
  await page.getByRole('button', { name: /ログアウト/ }).click()

  await register(page, secondEmail)
  await expect(page.getByRole('heading', { name: 'すべての記事' })).toBeVisible()
  await expect(articleCard(page, privateTitle)).toHaveCount(0)
})

test('user separation also blocks update and delete through the API', async ({ request }, testInfo) => {
  const owner = await registerByApi(request, uniqueEmail('owner', testInfo))
  const intruder = await registerByApi(request, uniqueEmail('intruder', testInfo))
  const created = await createArticleByApi(
    request,
    owner.accessToken,
    uniqueUrl('owner-api', testInfo),
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

async function openArticleModal(page: Page): Promise<void> {
  const addButton = page.getByRole('button', { name: '記事を追加', exact: true })
  if (await addButton.count()) {
    await addButton.click()
    return
  }

  await page.getByRole('button', { name: '最初の記事を追加' }).click()
}

async function createArticle(
  page: Page,
  input: { url: string, title: string, notes?: string, tags?: string[], rating?: number }
): Promise<void> {
  await openArticleModal(page)
  await page.getByRole('textbox', { name: 'URL', exact: true }).fill(input.url)
  await page.getByRole('textbox', { name: 'タイトル（任意）', exact: true }).fill(input.title)

  if (input.tags?.length) {
    for (const tag of input.tags) {
      await page.getByRole('textbox', { name: '新しいタグ', exact: true }).fill(tag)
      await page.getByRole('textbox', { name: '新しいタグ', exact: true }).press('Enter')
    }
  }

  if (input.rating && input.rating > 0) {
    await page.getByRole('button', { name: `おすすめ度 ${input.rating} を選択` }).click()
  }

  if (input.notes) {
    await page.getByRole('textbox', { name: 'メモ', exact: true }).fill(input.notes)
  }

  await page.getByRole('button', { name: '保存する' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
}

async function openArticleFromList(page: Page, title: string): Promise<void> {
  await articleCard(page, title).click()
  await expect(page.getByRole('heading', { level: 2, name: title })).toBeVisible()
}

function articleCard(page: Page, title: string) {
  return page.locator('.article-card').filter({ hasText: title })
}

function uniqueEmail(prefix: string, testInfo: TestInfo): string {
  return `${prefix}-${uniqueSuffix(testInfo)}@example.com`
}

function uniqueUrl(prefix: string, testInfo: TestInfo): string {
  return `https://example.com/?readstackE2e=${prefix}-${uniqueSuffix(testInfo)}`
}

function uniqueSuffix(testInfo: TestInfo): string {
  const titleSlug = testInfo.title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')

  return `${titleSlug.slice(0, 10)}-${RUN_ID}-w${testInfo.workerIndex}-r${testInfo.retry}`
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
