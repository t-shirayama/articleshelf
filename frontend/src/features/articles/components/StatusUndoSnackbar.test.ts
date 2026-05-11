/* eslint-disable vue/one-component-per-file */
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createApp, ref, type App } from 'vue'
import { i18n } from '../../../shared/i18n'
import StatusUndoSnackbar from './StatusUndoSnackbar.vue'

describe('StatusUndoSnackbar', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('forwards open state updates and undo actions', () => {
    const open = ref(true)
    const undo = vi.fn()
    const { root, app } = mountStatusUndoSnackbar(open, undo)

    root.querySelector<HTMLButtonElement>('.undo-button')?.click()
    root.querySelector<HTMLDivElement>('.snackbar')?.dispatchEvent(new CustomEvent('close'))

    expect(undo).toHaveBeenCalled()
    expect(open.value).toBe(false)

    app.unmount()
  })
})

function mountStatusUndoSnackbar(open: { value: boolean }, undo: () => void): { root: HTMLElement, app: App<Element> } {
  const root = document.createElement('div')
  document.body.append(root)
  const app = createApp({
    components: { StatusUndoSnackbar },
    setup() {
      return { open, undo }
    },
    template: '<StatusUndoSnackbar v-model:open="open" message="Saved" @undo="undo" />'
  })
  app.use(i18n)
  app.component('VSnackbar', {
    emits: ['update:modelValue'],
    template: '<div class="snackbar" @close="$emit(\'update:modelValue\', false)"><slot /><slot name="actions" /></div>'
  })
  app.component('VBtn', {
    template: '<button class="undo-button" @click="$emit(\'click\')"><slot /></button>'
  })
  app.mount(root)
  return { root, app }
}
