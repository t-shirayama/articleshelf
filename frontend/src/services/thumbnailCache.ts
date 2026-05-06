const DB_NAME = 'readstack-thumbnail-cache'
const DB_VERSION = 1
const STORE_NAME = 'thumbnails'
const FAILURE_RETRY_MS = 24 * 60 * 60 * 1000
const MAX_THUMBNAIL_BYTES = 5 * 1024 * 1024

interface ThumbnailCacheRecord {
  url: string
  blob?: Blob
  cachedAt?: number
  failedAt?: number
}

const pendingLoads = new Map<string, Promise<string | null>>()

export function loadThumbnailFromCache(url: string): Promise<string | null> {
  const pending = pendingLoads.get(url)
  if (pending) return pending

  const load = loadThumbnail(url)
    .catch(() => null)
    .finally(() => {
      pendingLoads.delete(url)
    })
  pendingLoads.set(url, load)
  return load
}

async function loadThumbnail(url: string): Promise<string | null> {
  if (!('indexedDB' in window)) return null

  const db = await openDatabase()
  const cached = await getRecord(db, url)
  if (cached?.blob) return URL.createObjectURL(cached.blob)
  if (cached?.failedAt && Date.now() - cached.failedAt < FAILURE_RETRY_MS) return null

  try {
    const response = await fetch(url, {
      cache: 'force-cache',
      mode: 'cors',
      referrerPolicy: 'no-referrer'
    })

    if (!response.ok) throw new Error(`Thumbnail request failed: ${response.status}`)

    const contentType = response.headers.get('content-type') || ''
    if (!contentType.startsWith('image/')) throw new Error(`Unsupported thumbnail content type: ${contentType}`)

    const blob = await response.blob()
    if (blob.size > MAX_THUMBNAIL_BYTES) throw new Error('Thumbnail is too large to cache')

    await putRecord(db, { url, blob, cachedAt: Date.now() })
    return URL.createObjectURL(blob)
  } catch {
    await putRecord(db, { url, failedAt: Date.now() })
    return null
  }
}

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onupgradeneeded = () => {
      const db = request.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'url' })
      }
    }

    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

function getRecord(db: IDBDatabase, url: string): Promise<ThumbnailCacheRecord | undefined> {
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, 'readonly')
    const store = transaction.objectStore(STORE_NAME)
    const request = store.get(url)

    request.onsuccess = () => resolve(request.result as ThumbnailCacheRecord | undefined)
    request.onerror = () => reject(request.error)
  })
}

function putRecord(db: IDBDatabase, record: ThumbnailCacheRecord): Promise<void> {
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, 'readwrite')
    const store = transaction.objectStore(STORE_NAME)
    const request = store.put(record)

    request.onsuccess = () => resolve()
    request.onerror = () => reject(request.error)
  })
}
