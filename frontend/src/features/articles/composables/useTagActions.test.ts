import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { useTagActions } from './useTagActions'

describe('useTagActions', () => {
  it('runs tag mutations and clears the saving flag', async () => {
    const store = createStore()
    const actions = useTagActions({
      store: store as unknown as Parameters<typeof useTagActions>[0]['store'],
      t: (key) => key,
      searchDraft: ref(''),
      navigateToList: vi.fn(),
      requestNavigation: (action) => action()
    })

    await actions.renameTag('tag-1', 'Vue')
    await actions.addTag('Testing')
    await actions.mergeTag('tag-1', 'tag-2')
    await actions.deleteTag('tag-1')

    expect(store.renameTag).toHaveBeenCalledWith('tag-1', 'Vue')
    expect(store.createTag).toHaveBeenCalledWith('Testing')
    expect(store.mergeTag).toHaveBeenCalledWith('tag-1', 'tag-2')
    expect(store.deleteTag).toHaveBeenCalledWith('tag-1')
    expect(actions.isSavingTag.value).toBe(false)
  })

  it('turns mutation failures into localized store errors', async () => {
    const store = createStore()
    store.renameTag.mockRejectedValueOnce(new Error('rename failed'))
    const actions = useTagActions({
      store: store as unknown as Parameters<typeof useTagActions>[0]['store'],
      t: (key) => key,
      searchDraft: ref(''),
      navigateToList: vi.fn(),
      requestNavigation: (action) => action()
    })

    await actions.renameTag('tag-1', 'Vue')

    expect(store.error).toBe('rename failed')
    expect(actions.isSavingTag.value).toBe(false)
  })

  it('opens the articles list filtered by a tag through guarded navigation', () => {
    const store = createStore()
    const searchDraft = ref('previous search')
    const navigateToList = vi.fn()
    const requestNavigation = vi.fn((action: () => void) => action())
    const actions = useTagActions({
      store: store as unknown as Parameters<typeof useTagActions>[0]['store'],
      t: (key) => key,
      searchDraft,
      navigateToList,
      requestNavigation
    })

    actions.openTagArticles('Vue')

    expect(requestNavigation).toHaveBeenCalled()
    expect(navigateToList).toHaveBeenCalled()
    expect(searchDraft.value).toBe('')
    expect(store.setSearch).toHaveBeenCalledWith('')
    expect(store.setAllArticles).toHaveBeenCalled()
    expect(store.setTags).toHaveBeenCalledWith(['Vue'])
  })
})

function createStore() {
  return {
    error: '',
    renameTag: vi.fn().mockResolvedValue(undefined),
    createTag: vi.fn().mockResolvedValue(undefined),
    mergeTag: vi.fn().mockResolvedValue(undefined),
    deleteTag: vi.fn().mockResolvedValue(undefined),
    setSearch: vi.fn(),
    setAllArticles: vi.fn().mockResolvedValue(undefined),
    setTags: vi.fn()
  }
}
