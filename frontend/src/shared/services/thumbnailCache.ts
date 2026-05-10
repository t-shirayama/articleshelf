const DB_NAME = 'articleshelf-thumbnail-cache'
const DB_VERSION = 2
const STORE_NAME = 'thumbnails'
const FAILURE_RETRY_MS = 24 * 60 * 60 * 1000
const MAX_THUMBNAIL_BYTES = 5 * 1024 * 1024
const MAX_CACHE_RECORDS = 200
const MAX_CACHE_BYTES = 50 * 1024 * 1024

interface ThumbnailCacheRecord {
  url: string
  imageBlob?: Blob
  cachedAt?: number
  failedAt?: number
}

const pendingLoads = new Map<string, Promise<string | null>>()

export interface ThumbnailCacheLimits {
  maxRecords: number
  maxBytes: number
  now: number
}

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
  if (cached?.imageBlob) return URL.createObjectURL(cached.imageBlob)
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

    const imageBlob = await response.blob()
    if (imageBlob.size > MAX_THUMBNAIL_BYTES) throw new Error('Thumbnail is too large to cache')

    await putRecord(db, { url, imageBlob, cachedAt: Date.now() })
    await evictOldRecords(db)
    return URL.createObjectURL(imageBlob)
  } catch {
    await putRecord(db, { url, failedAt: Date.now() })
    await evictOldRecords(db)
    return null
  }
}

export function selectThumbnailEvictionUrls(
  records: ThumbnailCacheRecord[],
  limits: ThumbnailCacheLimits = { maxRecords: MAX_CACHE_RECORDS, maxBytes: MAX_CACHE_BYTES, now: Date.now() }
): string[] {
  const expiredFailureUrls = new Set(
    records
      .filter((record) => !record.imageBlob && record.failedAt !== undefined && limits.now - record.failedAt >= FAILURE_RETRY_MS)
      .map((record) => record.url)
  )
  const remaining = records.filter((record) => !expiredFailureUrls.has(record.url))
  const evictionUrls = new Set(expiredFailureUrls)
  let totalBytes = remaining.reduce((sum, record) => sum + (record.imageBlob?.size || 0), 0)
  let totalRecords = remaining.length

  const lruRecords = [...remaining].sort((left, right) => recordTimestamp(left) - recordTimestamp(right))
  for (const record of lruRecords) {
    if (totalRecords <= limits.maxRecords && totalBytes <= limits.maxBytes) break
    evictionUrls.add(record.url)
    totalRecords -= 1
    totalBytes -= record.imageBlob?.size || 0
  }

  return [...evictionUrls]
}

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onupgradeneeded = () => {
      const db = request.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'url' })
      }

      const transaction = request.transaction
      if (transaction) normalizeLegacyRecords(transaction)
    }

    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

function normalizeLegacyRecords(transaction: IDBTransaction): void {
  const store = transaction.objectStore(STORE_NAME)
  const request = store.openCursor()

  request.onsuccess = () => {
    const cursor = request.result
    if (!cursor) return

    const record = cursor.value as ThumbnailCacheRecord & { blob?: Blob }
    const legacyBlob = record.blob instanceof Blob ? record.blob : undefined

    if (legacyBlob && !record.imageBlob) {
      cursor.update({
        url: record.url,
        imageBlob: legacyBlob,
        cachedAt: record.cachedAt,
        failedAt: record.failedAt
      } satisfies ThumbnailCacheRecord)
    }

    cursor.continue()
  }
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

function evictOldRecords(db: IDBDatabase): Promise<void> {
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, 'readwrite')
    const store = transaction.objectStore(STORE_NAME)
    const getAllRequest = store.getAll()

    getAllRequest.onsuccess = () => {
      const urls = selectThumbnailEvictionUrls(getAllRequest.result as ThumbnailCacheRecord[])
      for (const url of urls) {
        store.delete(url)
      }
    }
    getAllRequest.onerror = () => reject(getAllRequest.error)
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error)
    transaction.onabort = () => reject(transaction.error)
  })
}

function recordTimestamp(record: ThumbnailCacheRecord): number {
  return record.cachedAt || record.failedAt || 0
}
