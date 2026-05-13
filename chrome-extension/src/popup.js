const APP_BASE_URL = 'https://articleshelf.pages.dev'
const API_BASE_URL = 'https://articleshelf-api.onrender.com'
const CLIENT_ID = 'articleshelf-chrome-extension'
const EXTENSION_ID = 'bpkppkfmcfdpfbododebdbaaoodglnde'
const REDIRECT_URI = 'https://bpkppkfmcfdpfbododebdbaaoodglnde.chromiumapp.org/'

const TOKEN_STORAGE_KEY = 'articleshelfExtensionToken'
const AUTH_SESSION_KEY = 'articleshelfExtensionAuthSession'

const messages = {
  en: {
    documentTitle: 'ArticleShelf Quick Add',
    popupTitle: 'Quick Save',
    popupCopy: 'Save the current page directly to ArticleShelf with a short-lived extension token.',
    targetLabel: 'Target',
    loadingTitle: 'Loading current page...',
    loadingUrl: 'Preparing draft...',
    login: 'Log in to ArticleShelf',
    saveUnread: 'Save unread',
    saveRead: 'Save read',
    initializeError: 'Could not initialize the extension popup.',
    unsupportedTitle: 'This page cannot be saved',
    unsupportedUrl: 'Open a regular http or https article page, then try again.',
    unsupportedStatus: 'Chrome internal pages, extension pages, and blank tabs are not supported.',
    untitledPage: 'Untitled page',
    loginRequired: 'Log in to save this page to ArticleShelf.',
    openingLogin: 'Opening ArticleShelf login...',
    invalidLoginResponse: 'Could not verify the ArticleShelf login response.',
    loginComplete: 'Logged in. You can save this page now.',
    loginFailed: 'Could not log in to ArticleShelf.',
    savingRead: 'Saving as read...',
    savingUnread: 'Saving as unread...',
    savedRead: 'Saved as read.',
    savedUnread: 'Saved as unread.',
    alreadyRead: 'Already saved as read.',
    alreadyUnread: 'Already saved as unread.',
    updatedRead: 'Updated to read.',
    updatedUnread: 'Updated to unread.',
    tokenExchangeFailed: 'Could not exchange ArticleShelf authorization code.',
    loginExpired: 'ArticleShelf login expired. Please log in again.',
    saveFailed: 'Could not save this page.',
    ready: 'Ready to save this page.',
    invalidBaseUrl: 'Use an http or https URL for ArticleShelf.',
    apiFailed: (status) => `ArticleShelf API request failed (${status}).`
  },
  ja: {
    documentTitle: 'ArticleShelf クイック追加',
    popupTitle: 'クイック保存',
    popupCopy: '現在のページを、短命の拡張機能トークンで ArticleShelf に直接保存します。',
    targetLabel: '接続先',
    loadingTitle: '現在のページを読み込み中...',
    loadingUrl: '下書きを準備しています...',
    login: 'ArticleShelf にログイン',
    saveUnread: '未読で保存',
    saveRead: '既読で保存',
    initializeError: '拡張機能 popup を初期化できませんでした。',
    unsupportedTitle: 'このページは保存できません',
    unsupportedUrl: '通常の http または https の記事ページを開いてから、もう一度試してください。',
    unsupportedStatus: 'Chrome 内部ページ、拡張機能ページ、空のタブは対象外です。',
    untitledPage: '無題のページ',
    loginRequired: 'このページを ArticleShelf に保存するにはログインしてください。',
    openingLogin: 'ArticleShelf のログインを開いています...',
    invalidLoginResponse: 'ArticleShelf のログイン応答を確認できませんでした。',
    loginComplete: 'ログインしました。このページを保存できます。',
    loginFailed: 'ArticleShelf にログインできませんでした。',
    savingRead: '既読で保存しています...',
    savingUnread: '未読で保存しています...',
    savedRead: '既読で保存しました。',
    savedUnread: '未読で保存しました。',
    alreadyRead: 'すでに既読で保存されています。',
    alreadyUnread: 'すでに未読で保存されています。',
    updatedRead: '既読に更新しました。',
    updatedUnread: '未読に更新しました。',
    tokenExchangeFailed: 'ArticleShelf の認可コードをトークンに交換できませんでした。',
    loginExpired: 'ArticleShelf のログイン期限が切れました。もう一度ログインしてください。',
    saveFailed: 'このページを保存できませんでした。',
    ready: 'このページを保存できます。',
    invalidBaseUrl: 'ArticleShelf には http または https の URL を指定してください。',
    apiFailed: (status) => `ArticleShelf API リクエストに失敗しました (${status})。`
  }
}

const locale = detectLocale()
const t = messages[locale]

const popupTitle = document.querySelector('#popup-title')
const popupCopy = document.querySelector('#popup-copy')
const targetLabel = document.querySelector('#target-label')
const appBaseUrlDisplay = document.querySelector('#app-base-url-display')
const pageTitle = document.querySelector('#page-title')
const pageUrl = document.querySelector('#page-url')
const statusMessage = document.querySelector('#status-message')
const loginButton = document.querySelector('#login')
const saveUnreadButton = document.querySelector('#save-unread')
const saveReadButton = document.querySelector('#save-read')

let currentTab = null
let currentToken = null
let currentArticle = null
let currentPageSupported = false

localizeStaticText()

initialize().catch((error) => {
  setStatus(error instanceof Error ? error.message : t.initializeError)
  setBusy(false)
})

loginButton.addEventListener('click', () => {
  void login()
})
saveUnreadButton.addEventListener('click', () => {
  void saveArticle('UNREAD')
})
saveReadButton.addEventListener('click', () => {
  void saveArticle('READ')
})

async function initialize() {
  appBaseUrlDisplay.textContent = normalizeAppBaseUrl(APP_BASE_URL)
  setBusy(true)

  const [activeTab] = await chrome.tabs.query({ active: true, currentWindow: true })
  currentTab = activeTab ?? null

  if (!currentTab?.url || !isHttpUrl(currentTab.url)) {
    currentPageSupported = false
    pageTitle.textContent = t.unsupportedTitle
    pageUrl.textContent = t.unsupportedUrl
    currentToken = await readStoredToken()
    renderState(t.unsupportedStatus)
    return
  }

  currentPageSupported = true
  pageTitle.textContent = currentTab.title?.trim() || t.untitledPage
  pageUrl.textContent = currentTab.url
  currentToken = await readStoredToken()
  if (currentToken) {
    await lookupCurrentArticle()
  }
  renderState(currentToken ? statusForCurrentArticle() : t.loginRequired)
}

async function login() {
  setBusy(true, t.openingLogin)
  try {
    const verifier = randomBase64Url(32)
    const state = randomBase64Url(24)
    const challenge = await pkceChallenge(verifier)
    await chrome.storage.session.set({ [AUTH_SESSION_KEY]: { state, verifier } })

    const authorizeUrl = new URL('/extension/authorize', normalizeAppBaseUrl(APP_BASE_URL))
    authorizeUrl.searchParams.set('client_id', CLIENT_ID)
    authorizeUrl.searchParams.set('extension_id', EXTENSION_ID)
    authorizeUrl.searchParams.set('redirect_uri', REDIRECT_URI)
    authorizeUrl.searchParams.set('state', state)
    authorizeUrl.searchParams.set('code_challenge', challenge)
    authorizeUrl.searchParams.set('code_challenge_method', 'S256')

    const responseUrl = await chrome.identity.launchWebAuthFlow({
      url: authorizeUrl.toString(),
      interactive: true
    })
    const response = new URL(responseUrl)
    const code = response.searchParams.get('code')
    const returnedState = response.searchParams.get('state')
    const session = await readAuthSession()
    if (!code || !session || returnedState !== session.state) {
      throw new Error(t.invalidLoginResponse)
    }

    const token = await exchangeToken(code, session.verifier)
    await chrome.storage.session.remove(AUTH_SESSION_KEY)
    await storeToken(token)
    currentToken = token
    await lookupCurrentArticle()
    renderState(t.loginComplete)
  } catch (error) {
    renderState(error instanceof Error ? error.message : t.loginFailed)
  }
}

async function saveArticle(status) {
  if (!currentPageSupported || !currentToken || !currentTab?.url) return
  setBusy(true, status === 'READ' ? t.savingRead : t.savingUnread)
  try {
    if (!currentArticle) {
      currentArticle = await apiRequest('/api/extension/articles', {
        method: 'POST',
        body: JSON.stringify({
          url: currentTab.url,
          title: currentTab.title?.trim() || '',
          status,
          readDate: status === 'READ' ? today() : null
        })
      })
      renderState(status === 'READ' ? t.savedRead : t.savedUnread)
      return
    }

    if (currentArticle.status === status && (status === 'UNREAD' || currentArticle.readDate === today())) {
      renderState(status === 'READ' ? t.alreadyRead : t.alreadyUnread)
      return
    }

    currentArticle = await apiRequest(`/api/extension/articles/${currentArticle.id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({
        status,
        readDate: status === 'READ' ? today() : null
      })
    })
    renderState(status === 'READ' ? t.updatedRead : t.updatedUnread)
  } catch (error) {
    await handleApiError(error)
  }
}

async function lookupCurrentArticle() {
  currentArticle = null
  if (!currentToken || !currentTab?.url || !isHttpUrl(currentTab.url)) return
  try {
    currentArticle = await apiRequest(`/api/extension/articles/lookup?url=${encodeURIComponent(currentTab.url)}`)
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return
    await handleApiError(error)
  }
}

async function exchangeToken(code, verifier) {
  const response = await fetch(`${normalizeAppBaseUrl(API_BASE_URL)}/api/extension/oauth/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      grant_type: 'authorization_code',
      code,
      redirect_uri: REDIRECT_URI,
      client_id: CLIENT_ID,
      code_verifier: verifier
    })
  })
  const payload = await response.json().catch(() => null)
  if (!response.ok || !payload?.access_token) {
    throw new Error(t.tokenExchangeFailed)
  }
  return {
    accessToken: payload.access_token,
    expiresAt: Date.now() + (Number(payload.expires_in) || 86400) * 1000,
    scope: payload.scope || ''
  }
}

async function apiRequest(path, options = {}) {
  const response = await fetch(`${normalizeAppBaseUrl(API_BASE_URL)}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Accept-Language': locale === 'ja' ? 'ja-JP,ja;q=0.9,en;q=0.8' : 'en-US,en;q=0.9',
      Authorization: `Bearer ${currentToken.accessToken}`,
      ...(options.headers || {})
    }
  })
  const payload = await response.json().catch(() => null)
  if (!response.ok) throw new ApiError(response.status, payload)
  return payload
}

async function handleApiError(error) {
  if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
    await clearToken()
    currentToken = null
    currentArticle = null
    renderState(t.loginExpired)
    return
  }
  renderState(error instanceof Error ? error.message : t.saveFailed)
}

async function readStoredToken() {
  const stored = await chrome.storage.local.get(TOKEN_STORAGE_KEY)
  const token = stored[TOKEN_STORAGE_KEY]
  if (!token?.accessToken || !token?.expiresAt || token.expiresAt <= Date.now() + 30_000) {
    await clearToken()
    return null
  }
  return token
}

async function storeToken(token) {
  await chrome.storage.local.set({ [TOKEN_STORAGE_KEY]: token })
}

async function clearToken() {
  await chrome.storage.local.remove(TOKEN_STORAGE_KEY)
}

async function readAuthSession() {
  const stored = await chrome.storage.session.get(AUTH_SESSION_KEY)
  return stored[AUTH_SESSION_KEY] || null
}

function renderState(message) {
  setStatus(message)
  loginButton.hidden = Boolean(currentToken)
  loginButton.disabled = Boolean(currentToken)
  saveUnreadButton.hidden = !currentToken
  saveReadButton.hidden = !currentToken
  saveUnreadButton.disabled = !currentPageSupported || !currentToken
  saveReadButton.disabled = !currentPageSupported || !currentToken
}

function setBusy(busy, message = '') {
  if (message) setStatus(message)
  loginButton.disabled = busy
  saveUnreadButton.disabled = busy
  saveReadButton.disabled = busy
}

function statusForCurrentArticle() {
  if (!currentArticle) return t.ready
  return currentArticle.status === 'READ' ? t.alreadyRead : t.alreadyUnread
}

function normalizeAppBaseUrl(value) {
  const parsed = new URL(value)
  if (!isHttpUrl(parsed.toString())) {
    throw new Error(t.invalidBaseUrl)
  }
  parsed.pathname = '/'
  parsed.search = ''
  parsed.hash = ''
  return parsed.toString().replace(/\/$/, '')
}

function isHttpUrl(value) {
  try {
    const parsed = new URL(value)
    return parsed.protocol === 'http:' || parsed.protocol === 'https:'
  } catch {
    return false
  }
}

function today() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function setStatus(message) {
  statusMessage.textContent = message
}

function localizeStaticText() {
  document.documentElement.lang = locale
  document.title = t.documentTitle
  popupTitle.textContent = t.popupTitle
  popupCopy.textContent = t.popupCopy
  targetLabel.textContent = t.targetLabel
  pageTitle.textContent = t.loadingTitle
  pageUrl.textContent = t.loadingUrl
  loginButton.textContent = t.login
  saveUnreadButton.textContent = t.saveUnread
  saveReadButton.textContent = t.saveRead
}

function detectLocale() {
  const candidates = [
    chrome.i18n?.getUILanguage?.(),
    ...(navigator.languages || []),
    navigator.language
  ]

  return candidates.some((candidate) => String(candidate || '').toLowerCase().startsWith('ja')) ? 'ja' : 'en'
}

function randomBase64Url(length) {
  const bytes = new Uint8Array(length)
  crypto.getRandomValues(bytes)
  return base64Url(bytes)
}

async function pkceChallenge(verifier) {
  const bytes = new TextEncoder().encode(verifier)
  const digest = await crypto.subtle.digest('SHA-256', bytes)
  return base64Url(new Uint8Array(digest))
}

function base64Url(bytes) {
  let binary = ''
  for (const byte of bytes) binary += String.fromCharCode(byte)
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

class ApiError extends Error {
  constructor(status, payload) {
    super(payload?.messages?.join(', ') || t.apiFailed(status))
    this.status = status
    this.payload = payload
  }
}
