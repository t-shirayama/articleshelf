import AxeBuilder from '@axe-core/playwright'
import { expect, test, type APIRequestContext, type Page, type TestInfo } from '@playwright/test'

const RUN_ID = Date.now().toString(36)

test('user can register, create an article, logout and login again', async ({ page }, testInfo) => {
  const username = uniqueUsername('reader', testInfo)
  const articleTitle = `E2E article ${uniqueSuffix(testInfo)}`

  await register(page, username)
  await createArticle(page, {
    url: uniqueUrl('register-login', testInfo),
    title: articleTitle,
    notes: 'E2E memo'
  })

  await expect(articleCard(page, articleTitle)).toBeVisible()

  await page.getByRole('button', { name: /ログアウト/ }).click()
  await expect(page.getByRole('heading', { name: 'ログイン' })).toBeVisible()

  await login(page, username)
  await expect(articleCard(page, articleTitle)).toBeVisible()
})

test('user can register without a display name', async ({ page }, testInfo) => {
  const username = uniqueUsername('nodisplay', testInfo)

  await page.addInitScript(() => window.localStorage.setItem('articleshelf.locale', 'ja'))
  await page.goto('/')
  await page.getByRole('button', { name: '登録', exact: true }).click()
  await page.getByLabel('ユーザー名').fill(username)
  await page.getByLabel('パスワード').fill('password123')
  await page.getByRole('button', { name: '登録して始める' }).click()

  await expect(page.getByRole('heading', { name: 'すべての記事' })).toBeVisible()
  await expect(page.locator('.sidebar > .sidebar-account')).toContainText(username)
})

test('authenticated article list has no critical accessibility violations', async ({ page }, testInfo) => {
  const username = uniqueUsername('axe', testInfo)

  await register(page, username)

  const results = await new AxeBuilder({ page })
    .disableRules(['color-contrast'])
    .analyze()
  const blockingViolations = results.violations.filter((violation) => {
    return violation.impact === 'critical' || violation.impact === 'serious'
  })

  expect(blockingViolations).toEqual([])
})

test('user can change password and must use the new password', async ({ page }, testInfo) => {
  const username = uniqueUsername('password-change', testInfo)
  const newPassword = 'new-password123'

  await register(page, username)
  await page.getByRole('button', { name: 'アカウント' }).click()
  await page.getByLabel('現在のパスワード').first().fill('password123')
  await page.getByLabel('新しいパスワード').fill(newPassword)
  await page.getByRole('button', { name: 'パスワードを変更' }).click()

  await expect(page.getByRole('heading', { name: 'ログイン' })).toBeVisible()
  await page.getByLabel('ユーザー名').fill(username)
  await page.getByLabel('パスワード').fill('password123')
  await page.locator('form').getByRole('button', { name: 'ログイン' }).click()
  await expect(page.getByText('ユーザー名またはパスワードが正しくありません。')).toBeVisible()

  await login(page, username, newPassword)
})

test('user can log out all sessions and delete their account', async ({ page }, testInfo) => {
  const logoutAllUsername = uniqueUsername('logout-all', testInfo)
  const deleteUsername = uniqueUsername('delete-account', testInfo)

  await register(page, logoutAllUsername)
  await page.getByRole('button', { name: 'アカウント' }).click()
  await page.getByRole('button', { name: '全端末からログアウト' }).click()
  await expect(page.getByRole('heading', { name: 'ログイン' })).toBeVisible()

  await register(page, deleteUsername)
  await page.getByRole('button', { name: 'アカウント' }).click()
  await page.getByLabel('現在のパスワード').last().fill('password123')
  await page.getByRole('button', { name: '退会する' }).click()
  await expect(page.getByRole('heading', { name: 'ログイン' })).toBeVisible()

  await page.getByLabel('ユーザー名').fill(deleteUsername)
  await page.getByLabel('パスワード').fill('password123')
  await page.locator('form').getByRole('button', { name: 'ログイン' }).click()
  await expect(page.getByText('ユーザー名またはパスワードが正しくありません。')).toBeVisible()
})

test('duplicate article url can open the existing article from preview', async ({ page }, testInfo) => {
  const username = uniqueUsername('duplicate', testInfo)
  const sharedUrl = uniqueUrl('duplicate', testInfo)
  const originalTitle = `Original ${uniqueSuffix(testInfo)}`

  await register(page, username)
  await createArticle(page, {
    url: sharedUrl,
    title: originalTitle
  })

  await openArticleModal(page)
  const urlInput = page.getByRole('textbox', { name: 'URL', exact: true })
  await urlInput.fill(sharedUrl)
  await urlInput.blur()

  await expect(page.getByText('この記事はすでに保存されています')).toBeVisible()
  await expect(page.getByRole('button', { name: '保存する' })).toBeDisabled()
  await page.getByRole('button', { name: '登録済みの記事を開く' }).click()

  await expect(page.getByRole('heading', { level: 2, name: originalTitle })).toBeVisible()
  await expect(page.getByRole('dialog')).toHaveCount(0)
})

test('user can edit article details and persist notes tags and rating', async ({ page }, testInfo) => {
  const username = uniqueUsername('detail-edit', testInfo)
  const articleTitle = `Editable ${uniqueSuffix(testInfo)}`

  await register(page, username)
  await createArticle(page, {
    url: uniqueUrl('detail-edit', testInfo),
    title: articleTitle,
    notes: 'Before edit'
  })

  await openArticleFromList(page, articleTitle)
  await page.getByRole('button', { name: '編集', exact: true }).click()
  await page.getByRole('textbox', { name: 'メモ', exact: true }).fill('Edited memo with details')
  await page.getByRole('button', { name: '記事の詳細' }).click()
  await page.getByRole('textbox', { name: 'タイトル', exact: true }).fill(`${articleTitle} updated`)
  await page.getByRole('textbox', { name: '新しいタグ', exact: true }).fill('Vue')
  await page.getByRole('textbox', { name: '新しいタグ', exact: true }).press('Enter')
  await page.getByRole('button', { name: 'おすすめ度 4 を選択' }).click()
  await page.getByRole('textbox', { name: '概要', exact: true }).fill('Edited summary')
  await page.getByRole('button', { name: '保存', exact: true }).click()

  await expect(page.getByRole('heading', { level: 2, name: `${articleTitle} updated` })).toBeVisible()
  await expect(page.getByText('Edited memo with details')).toBeVisible()
  await expect(page.getByText('Vue')).toBeVisible()
  await expect(page.getByLabel('おすすめ度 4 / 5')).toBeVisible()
})

test('user can delete an article from the detail view', async ({ page }, testInfo) => {
  const username = uniqueUsername('detail-delete', testInfo)
  const articleTitle = `Delete me ${uniqueSuffix(testInfo)}`

  await register(page, username)
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
  const username = uniqueUsername('status-toggle', testInfo)
  const articleTitle = `Toggle ${uniqueSuffix(testInfo)}`

  await register(page, username)
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

test('user can favorite an article and filter to favorites', async ({ page }, testInfo) => {
  const username = uniqueUsername('favorite', testInfo)
  const favoriteTitle = `Favorite ${uniqueSuffix(testInfo)}`
  const otherTitle = `Plain ${uniqueSuffix(testInfo)}`

  await register(page, username)
  await createArticle(page, {
    url: uniqueUrl('favorite-on', testInfo),
    title: favoriteTitle
  })
  await createArticle(page, {
    url: uniqueUrl('favorite-off', testInfo),
    title: otherTitle
  })

  const favoriteButton = articleCard(page, favoriteTitle).locator('.card-favorite-button')
  await favoriteButton.click()
  await expect(favoriteButton).toHaveClass(/is-active/)

  await page.getByRole('button', { name: 'お気に入り', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'お気に入り' })).toBeVisible()
  await expect(articleCard(page, favoriteTitle)).toBeVisible()
  await expect(articleCard(page, otherTitle)).toHaveCount(0)
})

test('user can filter articles by search, tag and rating', async ({ page }, testInfo) => {
  const username = uniqueUsername('filter', testInfo)
  const matchingTitle = `Vue guide ${uniqueSuffix(testInfo)}`
  const nonMatchingTitle = `Java guide ${uniqueSuffix(testInfo)}`

  await register(page, username)
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
  await expect(filterDialog.locator('.tag-editor-chip', { hasText: 'Vue' })).toBeVisible()
  await filterDialog.getByRole('button', { name: '4 / 5', exact: true }).click()
  await expect(filterDialog.getByRole('button', { name: '4 / 5', exact: true })).toHaveClass(/is-active/)
  await filterDialog.getByRole('button', { name: '適用する' }).click()

  await expect(articleCard(page, matchingTitle)).toBeVisible()
  await expect(articleCard(page, nonMatchingTitle)).toHaveCount(0)
  await expect(page.locator('.filter-open-badge')).toHaveText('2')
  await expect(page.getByText('適用中')).toBeVisible()
})

test('user can sort articles by title', async ({ page }, testInfo) => {
  const username = uniqueUsername('sort', testInfo)
  const laterTitle = `Zeta ${uniqueSuffix(testInfo)}`
  const firstTitle = `Alpha ${uniqueSuffix(testInfo)}`

  await register(page, username)
  await createArticle(page, {
    url: uniqueUrl('sort-zeta', testInfo),
    title: laterTitle
  })
  await createArticle(page, {
    url: uniqueUrl('sort-alpha', testInfo),
    title: firstTitle
  })

  await page.locator('.search-filter-bar .sort-select').click()
  await page.getByRole('option', { name: 'タイトル順' }).click()

  await expect(page.locator('.article-card h3').first()).toHaveText(firstTitle)
})

test('user can open articles from the calendar in created and read modes', async ({ page, request }, testInfo) => {
  const username = uniqueUsername('calendar', testInfo)
  const articleTitle = `Calendar ${uniqueSuffix(testInfo)}`
  const futureArticleTitle = `Calendar future ${uniqueSuffix(testInfo)}`
  const futureReadDate = monthOffsetDateKey(1, 15)

  await register(page, username)
  await createArticle(page, {
    url: uniqueUrl('calendar', testInfo),
    title: articleTitle
  })
  const apiUser = await loginByApi(request, username, 'password123')
  await createArticleByApi(
    request,
    apiUser.accessToken,
    uniqueUrl('calendar-future', testInfo),
    futureArticleTitle,
    {
      status: 'READ',
      readDate: futureReadDate
    }
  )
  await page.reload()
  await expect(articleCard(page, articleTitle)).toBeVisible()
  await articleCard(page, articleTitle).locator('.status-toggle-button').click()
  await expect(page.getByText('既読にしました')).toBeVisible()

  await page.getByRole('button', { name: 'カレンダー', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'カレンダー' })).toBeVisible()
  await expect(page.getByRole('button', { name: articleTitle })).toBeVisible()

  await page.getByRole('button', { name: '既読日' }).click()
  await expect(page.getByRole('button', { name: articleTitle })).toBeVisible()
  await page.getByRole('button', { name: articleTitle }).click()

  await expect(page.getByRole('heading', { level: 2, name: articleTitle })).toBeVisible()
  await page.getByRole('button', { name: '戻る' }).click()
  await expect(page.getByRole('heading', { name: 'カレンダー' })).toBeVisible()
  await expect(page.getByRole('button', { name: articleTitle })).toBeVisible()

  await page.getByRole('button', { name: '次月' }).click()
  await expect(page.getByRole('button', { name: futureArticleTitle })).toBeVisible()
  await page.getByRole('button', { name: futureArticleTitle }).click()

  await expect(page.getByRole('heading', { level: 2, name: futureArticleTitle })).toBeVisible()
  await page.getByRole('button', { name: '戻る' }).click()
  await expect(page.getByRole('heading', { name: 'カレンダー' })).toBeVisible()
  await expect(page.getByRole('button', { name: futureArticleTitle })).toBeVisible()
})

test('user can render markdown notes safely in the detail view', async ({ page }, testInfo) => {
  const username = uniqueUsername('markdown', testInfo)
  const articleTitle = `Markdown ${uniqueSuffix(testInfo)}`

  await register(page, username)
  await createArticle(page, {
    url: uniqueUrl('markdown', testInfo),
    title: articleTitle,
    notes: [
      '[ArticleShelf docs](https://example.com/docs)',
      '',
      '```ts',
      'const answer = 42',
      '```',
      '',
      '<script>alert("xss")</script>'
    ].join('\n')
  })

  await openArticleFromList(page, articleTitle)
  const markdown = page.locator('.markdown-viewer')

  await expect(markdown.locator('a', { hasText: 'ArticleShelf docs' })).toHaveAttribute('href', 'https://example.com/docs')
  await expect(markdown.locator('code')).toContainText('const answer = 42')
  await expect(markdown.locator('script')).toHaveCount(0)
})

test('user is warned before leaving unsaved article edits', async ({ page }, testInfo) => {
  const username = uniqueUsername('unsaved', testInfo)
  const articleTitle = `Unsaved ${uniqueSuffix(testInfo)}`

  await register(page, username)
  await createArticle(page, {
    url: uniqueUrl('unsaved', testInfo),
    title: articleTitle,
    notes: 'Original note'
  })

  await openArticleFromList(page, articleTitle)
  await page.getByRole('button', { name: '編集', exact: true }).click()
  await page.getByRole('textbox', { name: 'メモ', exact: true }).fill('Unsaved draft note')
  await expect(page.getByRole('textbox', { name: 'メモ', exact: true })).toHaveValue('Unsaved draft note')

  await page.goBack()
  await expect(page.getByRole('dialog')).toContainText('未保存の編集があります')
  await page.getByRole('button', { name: '編集を続ける' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.getByRole('textbox', { name: 'メモ', exact: true })).toHaveValue('Unsaved draft note')

  await page.getByRole('button', { name: '戻る' }).click()

  await expect(page.getByRole('dialog')).toContainText('未保存の編集があります')
  await page.getByRole('button', { name: '編集を続ける' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.getByRole('textbox', { name: 'メモ', exact: true })).toHaveValue('Unsaved draft note')

  await page.getByRole('button', { name: '戻る' }).click()
  await page.getByRole('button', { name: '破棄して移動' }).click()
  await expect(page.getByRole('heading', { name: 'すべての記事' })).toBeVisible()
})

test('user can search tags and open articles from tag management', async ({ page }, testInfo) => {
  const username = uniqueUsername('tag-management', testInfo)
  const matchingTitle = `Tagged AI ${uniqueSuffix(testInfo)}`
  const nonMatchingTitle = `Tagged Ops ${uniqueSuffix(testInfo)}`

  await register(page, username)
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
  const username = uniqueUsername('tag-create', testInfo)
  const tagName = `Idea-${uniqueSuffix(testInfo)}`

  await register(page, username)
  await page.getByRole('button', { name: 'タグ管理' }).click()
  await expect(page.getByRole('heading', { name: /タグ管理/ })).toBeVisible()
  await page.getByRole('button', { name: '最初のタグを追加' }).click()

  const dialog = page.getByRole('dialog')
  await dialog.getByRole('textbox', { name: 'タグ名' }).fill(tagName)
  await dialog.getByRole('button', { name: '追加', exact: true }).click()

  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.locator('.tag-management-row').filter({ hasText: tagName })).toBeVisible()

  const tagRow = page.locator('.tag-management-row').filter({ hasText: tagName })
  await tagRow.getByRole('button', { name: '削除' }).click()
  await page.getByRole('button', { name: '削除する' }).click()

  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.locator('.tag-management-row').filter({ hasText: tagName })).toHaveCount(0)
})

test('user can rename and merge tags from tag management', async ({ page }, testInfo) => {
  const username = uniqueUsername('tag-ops', testInfo)
  const sourceTag = `Source-${uniqueSuffix(testInfo)}`
  const targetTag = `Target-${uniqueSuffix(testInfo)}`
  const renamedTarget = `Renamed-${uniqueSuffix(testInfo)}`
  const sourceTitle = `Source article ${uniqueSuffix(testInfo)}`
  const targetTitle = `Target article ${uniqueSuffix(testInfo)}`

  await register(page, username)
  await createArticle(page, {
    url: uniqueUrl('tag-ops-source', testInfo),
    title: sourceTitle,
    tags: [sourceTag]
  })
  await createArticle(page, {
    url: uniqueUrl('tag-ops-target', testInfo),
    title: targetTitle,
    tags: [targetTag]
  })

  await page.getByRole('button', { name: 'タグ管理' }).click()
  await expect(page.getByRole('heading', { name: /タグ管理/ })).toBeVisible()

  const targetRow = page.locator('.tag-management-row').filter({ hasText: targetTag })
  await targetRow.getByRole('button', { name: '編集' }).click()
  await page.locator('.tag-management-rename-input input').fill(renamedTarget)
  await page.getByRole('button', { name: 'タグ名を保存' }).click()
  await expect(page.locator('.tag-management-row').filter({ hasText: renamedTarget })).toBeVisible()
  await expect(page.locator('.tag-management-row').filter({ hasText: targetTag })).toHaveCount(0)

  const sourceRow = page.locator('.tag-management-row').filter({ hasText: sourceTag })
  await sourceRow.getByRole('button', { name: '統合' }).click()
  const mergeDialog = page.getByRole('dialog')
  await expect(mergeDialog).toContainText('タグを統合')
  await mergeDialog.locator('.articleshelf-select').click()
  await page.getByRole('option', { name: renamedTarget }).click()
  await mergeDialog.getByRole('button', { name: '統合する' }).click()

  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.locator('.tag-management-row').filter({ hasText: sourceTag })).toHaveCount(0)

  await page
    .locator('.tag-management-row')
    .filter({ hasText: renamedTarget })
    .getByRole('button', { name: `${renamedTarget} の記事一覧を開く` })
    .click()
  await expect(page.getByRole('heading', { level: 1, name: renamedTarget })).toBeVisible()
  await expect(articleCard(page, sourceTitle)).toBeVisible()
  await expect(articleCard(page, targetTitle)).toBeVisible()
})

test('user can switch to English and the choice persists', async ({ page }, testInfo) => {
  const username = uniqueUsername('english', testInfo)

  await page.addInitScript(() => window.localStorage.setItem('articleshelf.locale', 'en'))
  await page.goto('/')
  await page.getByRole('button', { name: 'Sign up', exact: true }).click()
  await page.getByLabel('Username').fill(username)
  await page.getByLabel('Display name').fill('E2E User')
  await page.getByLabel('Password').fill('password123')
  await page.getByRole('button', { name: 'Sign up and start' }).click()

  await expect(page.getByRole('heading', { name: 'All articles' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Tag management' })).toBeVisible()
  await expect(page.getByRole('button', { name: /Log out/ })).toBeVisible()

  await page.reload()
  await expect(page.getByRole('heading', { name: 'All articles' })).toBeVisible()

  await page
    .getByLabel('Language')
    .getByRole('button', { name: '日本語', exact: true })
    .click()
  await expect(page.getByRole('heading', { name: 'すべての記事' })).toBeVisible()
})

test('users cannot see each other articles', async ({ page }, testInfo) => {
  const firstUsername = uniqueUsername('first', testInfo)
  const secondUsername = uniqueUsername('second', testInfo)
  const privateTitle = `Private article ${uniqueSuffix(testInfo)}`

  await register(page, firstUsername)
  await createArticle(page, {
    url: uniqueUrl('private-first', testInfo),
    title: privateTitle
  })
  await expect(articleCard(page, privateTitle)).toBeVisible()
  await page.getByRole('button', { name: /ログアウト/ }).click()

  await register(page, secondUsername)
  await expect(page.getByRole('heading', { name: 'すべての記事' })).toBeVisible()
  await expect(articleCard(page, privateTitle)).toHaveCount(0)
})

test('user separation also blocks update and delete through the API', async ({ request }, testInfo) => {
  const owner = await registerByApi(request, uniqueUsername('owner', testInfo))
  const intruder = await registerByApi(request, uniqueUsername('intruder', testInfo))
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

test('admin can reset a user password through the API', async ({ page, request }, testInfo) => {
  const username = uniqueUsername('admin-reset', testInfo)
  const newPassword = 'reset-password123'

  await registerByApi(request, username)
  const admin = await loginByApi(request, 'owner', 'password123')
  await resetPasswordByApi(request, admin.accessToken, username, newPassword)

  await page.addInitScript(() => window.localStorage.setItem('articleshelf.locale', 'ja'))
  await page.goto('/')
  await login(page, username, newPassword)
})

async function register(page: Page, username: string): Promise<void> {
  await page.addInitScript(() => window.localStorage.setItem('articleshelf.locale', 'ja'))
  await page.goto('/')
  await page.getByRole('button', { name: '登録', exact: true }).click()
  await page.getByLabel('ユーザー名').fill(username)
  await page.getByLabel('表示名').fill('E2E User')
  await page.getByLabel('パスワード').fill('password123')
  await page.getByRole('button', { name: '登録して始める' }).click()
  await expect(page.getByRole('heading', { name: 'すべての記事' })).toBeVisible()
}

async function login(page: Page, username: string, password = 'password123'): Promise<void> {
  await page.getByLabel('ユーザー名').fill(username)
  await page.getByLabel('パスワード').fill(password)
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
  await openArticleFormDetails(page)
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

async function openArticleFormDetails(page: Page): Promise<void> {
  const titleInput = page.getByRole('textbox', { name: 'タイトル（任意）', exact: true })
  if (await titleInput.isVisible().catch(() => false)) return
  await page.getByRole('button', { name: /タグ・メモ・おすすめ度を追加/ }).click()
  await expect(titleInput).toBeVisible()
}

async function openArticleFromList(page: Page, title: string): Promise<void> {
  await articleCard(page, title).click()
  await expect(page.getByRole('heading', { level: 2, name: title })).toBeVisible()
}

function articleCard(page: Page, title: string) {
  return page.locator('.article-card').filter({ hasText: title })
}

function uniqueUsername(prefix: string, testInfo: TestInfo): string {
  return `${prefix}-${uniqueSuffix(testInfo)}`.slice(0, 32)
}

function uniqueUrl(prefix: string, testInfo: TestInfo): string {
  return `https://example.com/?articleshelfE2e=${prefix}-${uniqueSuffix(testInfo)}`
}

function uniqueSuffix(testInfo: TestInfo): string {
  const titleSlug = testInfo.title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')

  return `${titleSlug.slice(0, 10)}-${RUN_ID}-w${testInfo.workerIndex}-r${testInfo.retry}`
}

function monthOffsetDateKey(offset: number, day: number): string {
  const date = new Date()
  date.setMonth(date.getMonth() + offset, day)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const normalizedDay = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${normalizedDay}`
}

function apiUrl(path: string): string {
  return `${process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:18080'}${path}`
}

async function registerByApi(request: APIRequestContext, username: string): Promise<{ accessToken: string }> {
  const response = await request.post(apiUrl('/api/auth/register'), {
    data: {
      username,
      password: 'password123',
      displayName: 'E2E User'
    }
  })
  expect(response.ok()).toBeTruthy()
  const body = await response.json()
  return { accessToken: body.accessToken as string }
}

async function loginByApi(
  request: APIRequestContext,
  username: string,
  password: string
): Promise<{ accessToken: string }> {
  const response = await request.post(apiUrl('/api/auth/login'), {
    data: {
      username,
      password
    }
  })
  expect(response.ok()).toBeTruthy()
  const body = await response.json()
  return { accessToken: body.accessToken as string }
}

async function resetPasswordByApi(
  request: APIRequestContext,
  accessToken: string,
  username: string,
  newPassword: string
): Promise<void> {
  const response = await request.post(apiUrl(`/api/admin/users/${encodeURIComponent(username)}/password`), {
    headers: {
      Authorization: `Bearer ${accessToken}`
    },
    data: {
      newPassword
    }
  })
  expect(response.status()).toBe(204)
}

async function createArticleByApi(
  request: APIRequestContext,
  accessToken: string,
  url: string,
  title: string,
  overrides: Partial<Record<string, unknown>> = {}
): Promise<{ id: string, payload: Record<string, unknown> }> {
  const payload = {
    url,
    title,
    summary: '',
    status: 'UNREAD',
    favorite: false,
    rating: 0,
    notes: 'Private note',
    tags: ['private'],
    ...overrides
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
