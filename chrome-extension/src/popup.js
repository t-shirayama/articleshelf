const APP_BASE_URL = 'https://articleshelf.pages.dev'

const appBaseUrlDisplay = document.querySelector('#app-base-url-display')
const pageTitle = document.querySelector('#page-title')
const pageUrl = document.querySelector('#page-url')
const statusMessage = document.querySelector('#status-message')
const openDraftButton = document.querySelector('#open-draft')

let currentTab = null

initialize().catch((error) => {
  setStatus(error instanceof Error ? error.message : 'Could not initialize the extension popup.')
  openDraftButton.disabled = true
})

async function initialize() {
  appBaseUrlDisplay.textContent = normalizeAppBaseUrl(APP_BASE_URL)

  const [activeTab] = await chrome.tabs.query({ active: true, currentWindow: true })
  currentTab = activeTab ?? null

  if (!currentTab?.url || !isHttpUrl(currentTab.url)) {
    pageTitle.textContent = 'This page cannot be shared'
    pageUrl.textContent = 'Open a regular http or https article page, then try again.'
    openDraftButton.disabled = true
    setStatus('Chrome internal pages, extension pages, and blank tabs are not supported.')
    return
  }

  pageTitle.textContent = currentTab.title?.trim() || 'Untitled page'
  pageUrl.textContent = currentTab.url
  openDraftButton.disabled = false
  setStatus('Ready to open a draft in ArticleShelf.')
}

openDraftButton.addEventListener('click', async () => {
  if (!currentTab?.url || !isHttpUrl(currentTab.url)) return

  try {
    const target = new URL('/articles', normalizeAppBaseUrl(APP_BASE_URL))
    target.searchParams.set('source', 'extension')
    target.searchParams.set('articleUrl', currentTab.url)
    if (currentTab.title?.trim()) {
      target.searchParams.set('articleTitle', currentTab.title.trim())
    }

    await chrome.tabs.create({ url: target.toString() })
    window.close()
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Could not open ArticleShelf.')
  }
})

function normalizeAppBaseUrl(value) {
  const trimmed = (value || APP_BASE_URL).trim()
  const parsed = new URL(trimmed)
  if (!isHttpUrl(parsed.toString())) {
    throw new Error('Use an http or https URL for ArticleShelf.')
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

function setStatus(message) {
  statusMessage.textContent = message
}
