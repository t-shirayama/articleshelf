import { afterEach, describe, expect, it } from 'vitest'
import { createApp, nextTick, ref, type App } from 'vue'
import { setCurrentLocale } from '../../../shared/i18n'
import { useTagManagementState } from './useTagManagementState'
import type { Tag } from '../types'

describe('useTagManagementState', () => {
  afterEach(() => {
    setCurrentLocale('en')
  })

  it('filters and sorts tags by count or name', async () => {
    const { result, app } = mountTagState([
      tag('1', 'Vue', 4),
      tag('2', 'Java', 1),
      tag('3', 'Accessibility', 4)
    ])

    expect(result.sortedTags.value.map((item) => item.name)).toEqual(['Accessibility', 'Vue', 'Java'])

    result.sortMode.value = 'COUNT_ASC'
    await nextTick()
    expect(result.sortedTags.value.map((item) => item.name)).toEqual(['Java', 'Accessibility', 'Vue'])

    result.searchQuery.value = 'vu'
    result.sortMode.value = 'NAME_ASC'
    await nextTick()
    expect(result.sortedTags.value.map((item) => item.name)).toEqual(['Vue'])

    app.unmount()
  })

  it('manages add, rename, merge, and delete dialog state', () => {
    const { result, app } = mountTagState([tag('1', 'Vue', 0), tag('2', 'Testing', 2)])

    result.openAddDialog()
    expect(result.addDialogOpen.value).toBe(true)
    result.closeAddDialog(true)
    expect(result.addDialogOpen.value).toBe(true)
    result.closeAddDialog(false)
    expect(result.addDialogOpen.value).toBe(false)

    result.startRename(tag('1', 'Vue', 0))
    expect(result.renameTagId.value).toBe('1')
    expect(result.renameDraft.value).toBe('Vue')
    result.cancelRename()
    expect(result.renameTagId.value).toBe('')

    result.openMerge(tag('1', 'Vue', 0))
    expect(result.mergeOptions.value).toEqual([{ title: 'Testing (2)', value: '2' }])
    result.closeMerge()
    expect(result.mergeSource.value).toBeNull()

    result.requestDelete(tag('2', 'Testing', 2))
    expect(result.deleteCandidate.value).toBeNull()
    expect(result.deleteTooltip(tag('2', 'Testing', 2))).toBe('tags.deleteDisabledTooltip')
    result.requestDelete(tag('1', 'Vue', 0))
    expect(result.deleteCandidate.value?.id).toBe('1')
    expect(result.deleteTooltip(tag('1', 'Vue', 0))).toBe('tags.deleteTooltip')

    app.unmount()
  })
})

function mountTagState(tags: Tag[]) {
  let result!: ReturnType<typeof useTagManagementState>
  const app = createApp({
    setup() {
      result = useTagManagementState(ref(tags), (key, params) => params ? `${key}:${JSON.stringify(params)}` : key)
      return () => null
    }
  })
  const root = document.createElement('div')
  document.body.append(root)
  app.mount(root)
  return { result, app: app as App<Element> }
}

function tag(id: string, name: string, articleCount: number): Tag {
  return { id, name, articleCount }
}
