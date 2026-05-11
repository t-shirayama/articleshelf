/* eslint-disable vue/one-component-per-file */
import { afterEach, describe, expect, it } from 'vitest'
import { createApp, defineComponent, h, nextTick, ref, type App, type PropType } from 'vue'
import { i18n } from '../i18n'
import DateField from './DateField.vue'

describe('DateField', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('formats the display value and emits selected dates in API format', () => {
    const value = ref<string | null>('2026-05-11')
    const { root, app } = mountDateField(value)

    expect(root.querySelector<HTMLInputElement>('.date-input')?.value).toBe('2026/05/11')

    root.querySelector<HTMLButtonElement>('.date-picker-day')?.click()

    expect(value.value).toBe('2026-06-03')

    app.unmount()
  })

  it('opens from the icon button and clears the current date', async () => {
    const value = ref<string | null>('2026-05-11')
    const { root, app } = mountDateField(value, { clearable: true })

    root.querySelector<HTMLButtonElement>('.date-picker-button')?.click()
    await nextTick()
    expect(root.querySelector('.date-menu')?.getAttribute('data-open')).toBe('true')

    root.querySelector<HTMLButtonElement>('.clear-date')?.click()
    expect(value.value).toBeNull()

    app.unmount()
  })

  it('does not open the picker when disabled', () => {
    const value = ref<string | null>(null)
    const { root, app } = mountDateField(value, { disabled: true })

    root.querySelector<HTMLButtonElement>('.date-picker-button')?.click()

    expect(root.querySelector('.date-menu')?.getAttribute('data-open')).toBe('false')

    app.unmount()
  })
})

function mountDateField(
  value: { value: string | null },
  props: { clearable?: boolean, disabled?: boolean } = {}
): { root: HTMLElement, app: App<Element> } {
  const root = document.createElement('div')
  document.body.append(root)

  const app = createApp({
    components: { DateField },
    setup() {
      return { value, props }
    },
    template: `
      <DateField
        v-model="value"
        label="Read date"
        :clearable="props.clearable"
        :disabled="props.disabled"
      />
    `
  })
  app.use(i18n)
  app.component('VMenu', defineComponent({
    props: {
      modelValue: {
        type: Boolean,
        default: false
      }
    },
    emits: ['update:modelValue'],
    setup(props, { slots }) {
      return () => h('div', { class: 'date-menu', 'data-open': String(props.modelValue) }, [
        slots.activator?.({
          props: {
            onClick: () => undefined
          }
        }),
        slots.default?.()
      ])
    }
  }))
  app.component('VTextField', defineComponent({
    inheritAttrs: false,
    props: {
      modelValue: {
        type: String,
        default: ''
      },
      clearable: {
        type: Boolean,
        default: false
      },
      disabled: {
        type: Boolean,
        default: false
      }
    },
    emits: ['click', 'click:clear'],
    setup(props, { attrs, emit, slots }) {
      return () => h('div', [
        h('input', {
          ...attrs,
          class: 'date-input',
          value: props.modelValue,
          disabled: props.disabled,
          readonly: true,
          onClick: () => emit('click')
        }),
        props.clearable
          ? h('button', {
            class: 'clear-date',
            type: 'button',
            onClick: (event: MouseEvent) => emit('click:clear', event)
          }, 'clear')
          : null,
        slots['append-inner']?.()
      ])
    }
  }))
  app.component('VDatePicker', defineComponent({
    props: {
      modelValue: {
        type: [Date, String, null] as PropType<Date | string | null>,
        default: null
      }
    },
    emits: ['update:modelValue'],
    setup(_props, { emit }) {
      return () => h('button', {
        class: 'date-picker-day',
        type: 'button',
        onClick: () => emit('update:modelValue', new Date(2026, 5, 3))
      }, '2026-06-03')
    }
  }))
  app.mount(root)

  return { root, app }
}
