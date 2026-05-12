/* eslint-disable vue/one-component-per-file */
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, h, nextTick, ref, type App, type Component } from 'vue'
import { i18n } from '../../../shared/i18n'
import DeleteConfirmDialog from './DeleteConfirmDialog.vue'
import TagAddDialog from './TagAddDialog.vue'
import TagDeleteDialog from './TagDeleteDialog.vue'
import UnsavedChangesDialog from './UnsavedChangesDialog.vue'
import type { Article } from '../types'

describe('article dialogs', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('emits cancel and confirm from the delete dialog', () => {
    const cancel = vi.fn()
    const confirm = vi.fn()
    const { root, app } = mountDialogComponent({
      components: { DeleteConfirmDialog },
      setup: () => ({ article: article(), cancel, confirm }),
      template: '<DeleteConfirmDialog :article="article" @cancel="cancel" @confirm="confirm" />'
    })

    expect(root.textContent).toContain('記事を削除しますか？')
    expect(root.textContent).toContain('Test article')

    root.querySelector<HTMLButtonElement>('.dialog-close')?.click()
    root.querySelector<HTMLButtonElement>('.action-button-danger')?.click()

    expect(cancel).toHaveBeenCalled()
    expect(confirm).toHaveBeenCalled()

    app.unmount()
  })

  it('emits cancel and confirm from the unsaved changes dialog', () => {
    const cancel = vi.fn()
    const confirm = vi.fn()
    const { root, app } = mountDialogComponent({
      components: { UnsavedChangesDialog },
      setup: () => ({ cancel, confirm }),
      template: '<UnsavedChangesDialog open @cancel="cancel" @confirm="confirm" />'
    })

    expect(root.textContent).toContain('未保存の編集があります')

    root.querySelector<HTMLButtonElement>('.dialog-close')?.click()
    root.querySelector<HTMLButtonElement>('.action-button-danger')?.click()

    expect(cancel).toHaveBeenCalled()
    expect(confirm).toHaveBeenCalled()

    app.unmount()
  })

  it('updates draft and blocks blank tag confirmation via disabled button state', async () => {
    const draft = ref('')
    const cancel = vi.fn()
    const confirm = vi.fn()
    const { root, app } = mountDialogComponent({
      components: { TagAddDialog },
      setup: () => ({ draft, cancel, confirm }),
      template: '<TagAddDialog open v-model:draft="draft" :saving="false" @cancel="cancel" @confirm="confirm" />'
    })

    const addButton = root.querySelector<HTMLButtonElement>('.action-button-primary')
    expect(addButton?.disabled).toBe(true)

    setInput(root, 'Vue')
    await nextTick()
    expect(draft.value).toBe('Vue')
    expect(addButton?.disabled).toBe(false)

    root.querySelector<HTMLInputElement>('.dialog-text-field')?.dispatchEvent(new KeyboardEvent('keyup', {
      bubbles: true,
      key: 'Enter'
    }))
    root.querySelector<HTMLButtonElement>('.dialog-close')?.click()

    expect(confirm).toHaveBeenCalled()
    expect(cancel).toHaveBeenCalled()

    app.unmount()
  })

  it('shows tag add and delete processing labels while saving', () => {
    const draft = ref('Vue')
    const cancel = vi.fn()
    const confirm = vi.fn()
    const { root, app } = mountDialogComponent({
      components: { TagAddDialog, TagDeleteDialog },
      setup: () => ({ draft, candidate: { id: 'tag-1', name: 'Vue', articleCount: 0 }, cancel, confirm }),
      template: `
        <TagAddDialog open v-model:draft="draft" saving @cancel="cancel" @confirm="confirm" />
        <TagDeleteDialog :candidate="candidate" saving @cancel="cancel" @confirm="confirm" />
      `
    })

    expect(root.textContent).toContain('追加中...')
    expect(root.textContent).toContain('削除中...')
    expect(root.querySelectorAll<HTMLElement>('[role="status"]').length).toBe(2)
    expect(root.querySelector<HTMLInputElement>('.dialog-text-field')?.disabled).toBe(true)
    root.querySelectorAll<HTMLButtonElement>('.action-button-primary, .action-button-danger')
      .forEach((button) => expect(button.disabled).toBe(true))

    app.unmount()
  })
})

function mountDialogComponent(rootComponent: {
  components: Record<string, Component>
  setup: () => Record<string, unknown>
  template: string
}): { root: HTMLElement, app: App<Element> } {
  const root = document.createElement('div')
  document.body.append(root)
  const globalI18n = i18n.global as unknown as { locale: { value: string } }
  globalI18n.locale.value = 'ja'

  const app = createApp(rootComponent)
  app.use(i18n)
  app.component('VDialog', defineComponent({
    props: {
      modelValue: {
        type: Boolean,
        default: false
      }
    },
    emits: ['update:modelValue'],
    setup(props, { emit, slots }) {
      return () => h('section', { class: 'dialog', 'data-open': String(props.modelValue) }, [
        h('button', { class: 'dialog-close', type: 'button', onClick: () => emit('update:modelValue', false) }, 'close'),
        slots.default?.()
      ])
    }
  }))
  app.component('VCard', defineComponent({
    props: {
      title: {
        type: String,
        default: ''
      }
    },
    setup(props, { slots }) {
      return () => h('article', [
        props.title ? h('h2', props.title) : null,
        slots.default?.()
      ])
    }
  }))
  app.component('VCardTitle', defineComponent({
    setup(_props, { slots }) {
      return () => h('h2', slots.default?.())
    }
  }))
  app.component('VCardText', defineComponent({
    setup(_props, { slots }) {
      return () => h('div', slots.default?.())
    }
  }))
  app.component('VCardActions', defineComponent({
    setup(_props, { slots }) {
      return () => h('footer', slots.default?.())
    }
  }))
  app.component('VSpacer', defineComponent({
    setup() {
      return () => h('span')
    }
  }))
  app.component('VBtn', defineComponent({
    inheritAttrs: false,
    props: {
      disabled: {
        type: Boolean,
        default: false
      },
      loading: {
        type: Boolean,
        default: false
      }
    },
    emits: ['click'],
    setup(props, { attrs, emit, slots }) {
      return () => h('button', {
        ...attrs,
        type: 'button',
        disabled: props.disabled || props.loading,
        onClick: () => emit('click')
      }, slots.default?.())
    }
  }))
  app.component('VTextField', defineComponent({
    props: {
      modelValue: {
        type: String,
        default: ''
      },
      disabled: {
        type: Boolean,
        default: false
      }
    },
    emits: ['update:modelValue', 'keyup.enter'],
    setup(props, { emit }) {
      return () => h('input', {
        class: 'dialog-text-field',
        value: props.modelValue,
        disabled: props.disabled,
        onInput: (event: Event) => emit('update:modelValue', (event.target as HTMLInputElement).value),
        onKeyup: (event: KeyboardEvent) => {
          if (event.key === 'Enter') emit('keyup.enter')
        }
      })
    }
  }))
  app.mount(root)

  return { root, app }
}

function setInput(root: HTMLElement, value: string): void {
  const input = root.querySelector<HTMLInputElement>('.dialog-text-field')
  if (!input) throw new Error('Missing dialog text field')
  input.value = value
  input.dispatchEvent(new Event('input', { bubbles: true }))
}

function article(): Article {
  return {
    id: 'article-1',
    version: 0,
    url: 'https://example.com',
    title: 'Test article',
    summary: '',
    thumbnailUrl: '',
    status: 'UNREAD',
    readDate: null,
    favorite: false,
    rating: 0,
    notes: '',
    tags: [],
    createdAt: '2026-05-11T00:00:00Z',
    updatedAt: '2026-05-11T00:00:00Z',
  }
}
