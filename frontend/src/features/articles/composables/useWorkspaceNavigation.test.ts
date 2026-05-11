import { describe, expect, it, vi } from 'vitest'
import { useWorkspaceNavigation } from './useWorkspaceNavigation'

describe('useWorkspaceNavigation', () => {
  it('navigates between workspace views and reports view changes', () => {
    const onViewChange = vi.fn()
    const navigation = useWorkspaceNavigation(onViewChange)

    navigation.navigateToCalendar()
    navigation.navigateToTags()
    navigation.navigateToList()

    expect(navigation.viewMode.value).toBe('list')
    expect(navigation.isListMode.value).toBe(true)
    expect(navigation.isCalendarActive.value).toBe(false)
    expect(navigation.isTagsActive.value).toBe(false)
    expect(onViewChange).toHaveBeenCalledTimes(3)
  })

  it('holds navigation behind an unsaved changes confirmation', () => {
    const navigation = useWorkspaceNavigation()
    const action = vi.fn(() => {
      navigation.viewMode.value = 'list'
    })
    navigation.viewMode.value = 'detail'
    navigation.detailHasUnsavedChanges.value = true

    navigation.requestNavigation(action)

    expect(action).not.toHaveBeenCalled()
    expect(navigation.unsavedChangesDialogOpen.value).toBe(true)

    navigation.confirmPendingNavigation()

    expect(action).toHaveBeenCalled()
    expect(navigation.detailHasUnsavedChanges.value).toBe(false)
    expect(navigation.unsavedChangesDialogOpen.value).toBe(false)
  })

  it('can cancel and reset pending navigation', () => {
    const navigation = useWorkspaceNavigation()
    const action = vi.fn()
    navigation.viewMode.value = 'detail'
    navigation.detailHasUnsavedChanges.value = true

    navigation.requestNavigation(action)
    navigation.cancelPendingNavigation()
    navigation.confirmPendingNavigation()

    expect(action).not.toHaveBeenCalled()

    navigation.resetNavigation()

    expect(navigation.viewMode.value).toBe('list')
    expect(navigation.detailHasUnsavedChanges.value).toBe(false)
    expect(navigation.unsavedChangesDialogOpen.value).toBe(false)
  })
})
