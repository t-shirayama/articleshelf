/* eslint-disable vue/one-component-per-file */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, h, nextTick, reactive, type App, type PropType } from 'vue'
import AuthScreen from './AuthScreen.vue'
import { i18n } from '../../../shared/i18n'

const routeState = reactive<{ path: string, query: Record<string, string> }>({ path: '/login', query: {} })
const routerPush = vi.fn((path: string) => {
  routeState.path = path
  return Promise.resolve()
})

const authStore = reactive({
  loading: false,
  error: '',
  login: vi.fn(),
  register: vi.fn()
})

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({ push: routerPush })
}))

vi.mock('../stores/auth', () => ({
  useAuthStore: () => authStore
}))

describe('AuthScreen', () => {
  beforeEach(() => {
    const globalI18n = i18n.global as unknown as { locale: { value: string } }
    globalI18n.locale.value = 'ja'
    routeState.path = '/login'
    routeState.query = {}
    routerPush.mockClear()
    authStore.loading = false
    authStore.error = ''
    authStore.login = vi.fn().mockResolvedValue(undefined)
    authStore.register = vi.fn().mockResolvedValue(undefined)
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('makes login and registration switching explicit', async () => {
    const { root, app } = mountAuthScreen()

    expect(root.textContent).toContain('ログイン / 登録の切り替え')
    expect(root.textContent).not.toContain('利用したい操作を選んでください')
    expect(root.querySelector('#auth-display-name')).toBeNull()

    modeButton(root, 'register')?.click()
    await nextTick()

    expect(routerPush).toHaveBeenCalledWith('/register')
    expect(root.textContent).toContain('ユーザー登録')
    expect(root.querySelector('#auth-display-name')).not.toBeNull()

    modeButton(root, 'login')?.click()
    await nextTick()

    expect(routerPush).toHaveBeenCalledWith('/login')
    expect(root.textContent).toContain('ログイン')
    expect(root.querySelector('#auth-display-name')).toBeNull()

    app.unmount()
  })

  it('submits login and register payloads for the selected mode', async () => {
    const { root, app } = mountAuthScreen()

    setInput(root, '#auth-username', ' Reader_1 ')
    setInput(root, '#auth-password', 'password123')
    submit(root)
    await nextTick()

    expect(authStore.login).toHaveBeenCalledWith({
      username: 'reader_1',
      password: 'password123'
    })

    modeButton(root, 'register')?.click()
    await nextTick()
    setInput(root, '#auth-username', ' New.User ')
    setInput(root, '#auth-display-name', 'New Reader')
    setInput(root, '#auth-password', 'new-password123')
    submit(root)
    await nextTick()

    expect(authStore.register).toHaveBeenCalledWith({
      username: 'new.user',
      displayName: 'New Reader',
      password: 'new-password123'
    })

    app.unmount()
  })

  it('returns to the requested route after login', async () => {
    routeState.query = { returnTo: '/articles?source=extension&articleUrl=https%3A%2F%2Fexample.com' }
    const { root, app } = mountAuthScreen()

    setInput(root, '#auth-username', 'reader_1')
    setInput(root, '#auth-password', 'password123')
    submit(root)
    await nextTick()

    expect(routerPush).toHaveBeenCalledWith('/articles?source=extension&articleUrl=https%3A%2F%2Fexample.com')

    app.unmount()
  })

  it('shows processing labels and blocks repeated login submission while loading', async () => {
    authStore.loading = true
    const { root, app } = mountAuthScreen()

    expect(root.textContent).toContain('ログイン中...')
    expect(root.querySelector<HTMLFormElement>('form')?.getAttribute('aria-busy')).toBe('true')
    expect(root.querySelector<HTMLElement>('[role="status"]')?.textContent).toContain('ログイン中...')

    setInput(root, '#auth-username', 'reader_1')
    setInput(root, '#auth-password', 'password123')
    submit(root)
    await nextTick()

    expect(authStore.login).not.toHaveBeenCalled()
    expect(root.querySelector<HTMLInputElement>('#auth-username')?.disabled).toBe(true)
    expect(modeButton(root, 'register')?.disabled).toBe(true)

    app.unmount()
  })

  it('shows the registration processing label while registering', async () => {
    routeState.path = '/register'
    authStore.loading = true
    const { root, app } = mountAuthScreen({ mode: 'register' })

    expect(root.textContent).toContain('登録中...')
    expect(root.querySelector<HTMLElement>('[role="status"]')?.textContent).toContain('登録中...')
    expect(root.querySelector<HTMLInputElement>('#auth-display-name')?.disabled).toBe(true)

    app.unmount()
  })
})

function mountAuthScreen(props: Record<string, unknown> = {}): { root: HTMLElement, app: App<Element> } {
  const root = document.createElement('div')
  document.body.append(root)

  const app = createApp(AuthScreen, props)
  app.use(i18n)
  app.component('VBtnToggle', defineComponent({
    props: {
      modelValue: {
        type: String,
        default: ''
      }
    },
    emits: ['update:modelValue'],
    setup(_props, { emit, slots }) {
      function updateMode(event: MouseEvent): void {
        const button = (event.target as HTMLElement).closest('button')
        const value = button?.getAttribute('value')
        if (value) emit('update:modelValue', value)
      }

      return () => h('div', { class: 'v-btn-toggle', onClick: updateMode }, slots.default?.())
    }
  }))
  app.component('VBtn', defineComponent({
    inheritAttrs: false,
    setup(_props, { attrs, slots }) {
      return () => h('button', { ...attrs, type: 'button' }, slots.default?.())
    }
  }))
  app.component('VTextField', defineComponent({
    inheritAttrs: false,
    props: {
      modelValue: {
        type: String,
        default: ''
      },
      hint: {
        type: String,
        default: ''
      },
      errorMessages: {
        type: Array as PropType<string[]>,
        default: () => []
      }
    },
    emits: ['update:modelValue'],
    setup(props, { attrs, emit }) {
      function updateValue(event: Event): void {
        emit('update:modelValue', (event.target as HTMLInputElement).value)
      }

      return () => h('div', [
        h('input', { ...attrs, value: props.modelValue, onInput: updateValue }),
        props.hint ? h('small', props.hint) : null,
        props.errorMessages.map((message) => h('span', { key: message }, message))
      ])
    }
  }))
  app.mount(root)

  return { root, app }
}

function modeButton(root: HTMLElement, mode: string): HTMLButtonElement | null {
  return root.querySelector(`button[value="${mode}"]`)
}

function setInput(root: HTMLElement, selector: string, value: string): void {
  const input = root.querySelector<HTMLInputElement>(selector)
  if (!input) throw new Error(`Missing input: ${selector}`)
  input.value = value
  input.dispatchEvent(new Event('input', { bubbles: true }))
}

function submit(root: HTMLElement): void {
  root.querySelector<HTMLFormElement>('form')?.dispatchEvent(
    new Event('submit', { bubbles: true, cancelable: true })
  )
}
