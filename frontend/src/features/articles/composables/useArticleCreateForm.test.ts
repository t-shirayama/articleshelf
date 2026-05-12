import { afterEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import { useArticleCreateForm } from './useArticleCreateForm'

describe('useArticleCreateForm', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('returns null and exposes URL validation when URL is empty', () => {
    const form = useArticleCreateForm(ref(false), ref([]), (key) => key)

    expect(form.createSubmitInput(false)).toBeNull()
    expect(form.urlError.value).toBe('articleForm.validation.urlRequired')
  })

  it('requires read date when saving as read', () => {
    const form = useArticleCreateForm(ref(false), ref([]), (key) => key)

    form.form.url = 'https://example.com'
    form.form.readLater = false

    expect(form.createSubmitInput(false)).toBeNull()
    expect(form.readDateError.value).toBe('articleForm.validation.readDateRequired')
  })

  it('creates normalized article input when valid', () => {
    const form = useArticleCreateForm(ref(false), ref([]), (key) => key)

    form.form.url = ' https://example.com '
    form.form.readLater = false
    form.form.readDate = '2026-05-10'
    form.form.tags = [' Vue ', 'Vue', '']

    expect(form.createSubmitInput(false)).toMatchObject({
      url: 'https://example.com',
      status: 'READ',
      readDate: '2026-05-10',
      tags: ['Vue'],
    })
  })

  it('resets state and focuses the URL input when opened', async () => {
    const open = ref(false)
    const focus = vi.fn()
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((callback: FrameRequestCallback) => {
      callback(0)
      return 1
    })

    const form = useArticleCreateForm(open, ref([]), (key) => key)
    form.form.url = 'https://example.com/old'
    form.submitted.value = true
    form.urlInput.value = { focus }

    open.value = true
    await nextTick()
    await nextTick()

    expect(form.form.url).toBe('')
    expect(form.submitted.value).toBe(false)
    expect(focus).toHaveBeenCalled()
  })

  it('restores focus to the opener when closed', async () => {
    const open = ref(false)
    const button = document.createElement('button')
    document.body.append(button)
    button.focus()

    const form = useArticleCreateForm(open, ref([]), (key) => key)
    open.value = true
    await nextTick()

    const focus = vi.spyOn(button, 'focus')
    open.value = false
    await nextTick()
    await nextTick()

    expect(focus).toHaveBeenCalled()
    expect(form.createSubmitInput(true)).toBeNull()
  })

  it('deduplicates available tag options', () => {
    const tags = ref([
      { name: 'Vue' },
      { name: 'Testing' },
      { name: 'Vue' },
      { name: '' },
    ])
    const form = useArticleCreateForm(ref(false), tags, (key) => key)

    expect(form.tagOptions.value).toEqual(['Vue', 'Testing'])
  })

  it('applies initial seed values when opened from an external draft', async () => {
    const open = ref(false)
    const seed = ref({ url: 'https://example.com/from-extension', title: 'Draft title' })
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((callback: FrameRequestCallback) => {
      callback(0)
      return 1
    })

    const form = useArticleCreateForm(open, ref([]), (key) => key, seed)

    open.value = true
    await nextTick()
    await nextTick()

    expect(form.form.url).toBe('https://example.com/from-extension')
    expect(form.form.title).toBe('Draft title')
  })
})
