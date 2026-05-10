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
})

function record(url: string, size: number, cachedAt: number) {
  return {
    url,
    imageBlob: new Blob(['x'.repeat(size)]),
    cachedAt,
  }
}
