import { afterEach, describe, expect, it, vi } from 'vitest'
import { createApp } from 'vue'
import { i18n, setCurrentLocale } from '../../../shared/i18n'
import { useMotivationRotation } from './useMotivationRotation'

describe('useMotivationRotation', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    setCurrentLocale('en')
  })

  it('starts from a random motivation card', () => {
    vi.spyOn(Math, 'random').mockReturnValue(0.5)
    const { rotation, app } = mountRotation()

    expect(rotation.currentMotivation.value.id).not.toBe(1)
    app.unmount()
  })

  it('rotates to a different motivation card', () => {
    vi.spyOn(Math, 'random').mockReturnValueOnce(0).mockReturnValueOnce(0.9)
    const { rotation, app } = mountRotation()
    const first = rotation.currentMotivation.value.id

    rotation.rotateMotivation()

    expect(rotation.currentMotivation.value.id).not.toBe(first)
    app.unmount()
  })

  it('uses localized motivation card text', () => {
    setCurrentLocale('ja')
    const { rotation, app } = mountRotation()

    expect(rotation.currentMotivation.value.title).not.toBe('')
    expect(rotation.currentMotivation.value.note).not.toBe('')

    app.unmount()
  })
})

function mountRotation() {
  let rotation!: ReturnType<typeof useMotivationRotation>
  const app = createApp({
    setup() {
      rotation = useMotivationRotation()
      return () => null
    }
  })
  app.use(i18n)
  const root = document.createElement('div')
  document.body.append(root)
  app.mount(root)
  return { rotation, app }
}
