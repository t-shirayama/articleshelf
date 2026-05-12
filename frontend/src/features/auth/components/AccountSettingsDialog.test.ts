/* eslint-disable vue/one-component-per-file */
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, h, nextTick, ref, type App } from 'vue'
import { i18n } from '../../../shared/i18n'
import AccountSettingsDialog from './AccountSettingsDialog.vue'

describe('AccountSettingsDialog', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('shows action-specific processing labels and locks repeated account actions', async () => {
    const changePassword = vi.fn()
    const { root, app } = mountAccountSettingsDialog({
      onChangePassword: changePassword
    })

    setInput(root, 'input[autocomplete="current-password"]', 'password123')
    setInput(root, 'input[autocomplete="new-password"]', 'new-password123')
    await nextTick()
    buttonByText(root, 'パスワードを変更')?.dispatchEvent(
      new MouseEvent('click', { bubbles: true })
    )
    await nextTick()

    expect(changePassword).toHaveBeenCalledWith({
      currentPassword: 'password123',
      newPassword: 'new-password123'
    })
    expect(root.textContent).toContain('変更中...')
    expect(root.querySelector<HTMLElement>('[role="status"]')?.textContent).toContain('変更中...')
    expect(buttonByText(root, '変更中...')?.disabled).toBe(true)
    expect(buttonByText(root, '全端末からログアウト')?.disabled).toBe(true)
    expect(buttonByText(root, '退会する')?.disabled).toBe(true)

    app.unmount()
  })

  it('shows logout-all and delete processing labels', async () => {
    const logoutAll = vi.fn()
    const deleteAccount = vi.fn()
    const { root, app } = mountAccountSettingsDialog({ onLogoutAll: logoutAll, onDeleteAccount: deleteAccount })

    buttonByText(root, '全端末からログアウト')?.dispatchEvent(
      new MouseEvent('click', { bubbles: true })
    )
    await nextTick()
    expect(logoutAll).toHaveBeenCalled()
    expect(root.textContent).toContain('ログアウト中...')

    app.unmount()

    const second = mountAccountSettingsDialog({ onDeleteAccount: deleteAccount })
    setInput(second.root, 'input[data-delete-password="true"]', 'password123')
    await nextTick()
    buttonByText(second.root, '退会する')?.dispatchEvent(
      new MouseEvent('click', { bubbles: true })
    )
    await nextTick()

    expect(deleteAccount).toHaveBeenCalledWith({ currentPassword: 'password123' })
    expect(second.root.textContent).toContain('退会処理中...')

    second.app.unmount()
  })
})

function mountAccountSettingsDialog(handlers: {
  onChangePassword?: (input: { currentPassword: string; newPassword: string }) => void
  onLogoutAll?: () => void
  onDeleteAccount?: (input: { currentPassword: string }) => void
} = {}): { root: HTMLElement, app: App<Element> } {
  const root = document.createElement('div')
  document.body.append(root)
  const globalI18n = i18n.global as unknown as { locale: { value: string } }
  globalI18n.locale.value = 'ja'
  const loading = ref(false)

  const app = createApp({
    components: { AccountSettingsDialog },
    setup() {
      return {
        loading,
        changePassword: (input: { currentPassword: string; newPassword: string }) => {
          handlers.onChangePassword?.(input)
          loading.value = true
        },
        logoutAll: () => {
          handlers.onLogoutAll?.()
          loading.value = true
        },
        deleteAccount: (input: { currentPassword: string }) => {
          handlers.onDeleteAccount?.(input)
          loading.value = true
        }
      }
    },
    template: `
      <AccountSettingsDialog
        open
        :loading="loading"
        error=""
        @change-password="changePassword"
        @logout-all="logoutAll"
        @delete-account="deleteAccount"
      />
    `
  })
  app.use(i18n)
  app.component('VDialog', passthrough('section'))
  app.component('VCard', passthrough('article'))
  app.component('VCardText', passthrough('div'))
  app.component('VDivider', passthrough('hr'))
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
    inheritAttrs: false,
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
    emits: ['update:modelValue'],
    setup(props, { attrs, emit }) {
      return () => h('input', {
        ...attrs,
        value: props.modelValue,
        disabled: props.disabled,
        onInput: (event: Event) => emit('update:modelValue', (event.target as HTMLInputElement).value)
      })
    }
  }))
  app.mount(root)

  return { root, app }
}

function passthrough(tag: string) {
  return defineComponent({
    inheritAttrs: false,
    setup(_props, { attrs, slots }) {
      return () => h(tag, attrs, slots.default?.())
    }
  })
}

function setInput(root: HTMLElement, selector: string, value: string): void {
  const input = root.querySelector<HTMLInputElement>(selector)
  if (!input) throw new Error(`Missing input: ${selector}`)
  input.value = value
  input.dispatchEvent(new Event('input', { bubbles: true }))
}

function buttonByText(root: HTMLElement, text: string): HTMLButtonElement | undefined {
  return Array.from(root.querySelectorAll<HTMLButtonElement>('button'))
    .find((button) => button.textContent?.includes(text))
}
