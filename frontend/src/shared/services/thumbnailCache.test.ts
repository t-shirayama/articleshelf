import { describe, expect, it } from 'vitest'
import { selectThumbnailEvictionUrls } from './thumbnailCache'

describe('thumbnailCache eviction', () => {
  it('evicts oldest records when count limit is exceeded', () => {
    const records = [
      record('old', 100, 1000),
      record('middle', 100, 2000),
      record('new', 100, 3000),
    ]

    expect(selectThumbnailEvictionUrls(records, { maxRecords: 2, maxBytes: 1000, now: 4000 })).toEqual(['old'])
  })

  it('evicts oldest image records until byte limit is satisfied', () => {
    const records = [
      record('old', 400, 1000),
      record('middle', 400, 2000),
      record('new', 400, 3000),
    ]

    expect(selectThumbnailEvictionUrls(records, { maxRecords: 10, maxBytes: 700, now: 4000 })).toEqual(['old', 'middle'])
  })

  it('evicts expired failure records before retrying a failed thumbnail', () => {
    const now = 48 * 60 * 60 * 1000
    const records = [
      { url: 'expired-failure', failedAt: 0 },
      { url: 'fresh-failure', failedAt: now },
    ]

    expect(selectThumbnailEvictionUrls(records, { maxRecords: 10, maxBytes: 1000, now })).toEqual(['expired-failure'])
  })

  it('keeps records when both count and byte limits are satisfied', () => {
    const records = [
      record('image', 100, 1000),
      { url: 'fresh-failure', failedAt: 2000 },
    ]

    expect(selectThumbnailEvictionUrls(records, { maxRecords: 2, maxBytes: 100, now: 3000 })).toEqual([])
  })

  it('uses failed timestamps for least-recently-used eviction order', () => {
    const records = [
      { url: 'old-failure', failedAt: 1000 },
      record('image', 100, 2000),
      { url: 'new-failure', failedAt: 3000 },
    ]

    expect(selectThumbnailEvictionUrls(records, { maxRecords: 2, maxBytes: 1000, now: 4000 })).toEqual(['old-failure'])
  })

  it('counts records without timestamps as oldest eviction candidates', () => {
    const records = [
      { url: 'legacy-empty' },
      record('new-image', 100, 2000),
    ]

    expect(selectThumbnailEvictionUrls(records, { maxRecords: 1, maxBytes: 1000, now: 3000 })).toEqual(['legacy-empty'])
  })
})

function record(url: string, size: number, cachedAt: number) {
  return {
    url,
    imageBlob: new Blob(['x'.repeat(size)]),
    cachedAt,
  }
}
