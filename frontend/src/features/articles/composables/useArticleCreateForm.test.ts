import { describe, expect, it } from 'vitest'
import { ref } from 'vue'
import { useArticleCreateForm } from './useArticleCreateForm'

describe('useArticleCreateForm', () => {
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
})
