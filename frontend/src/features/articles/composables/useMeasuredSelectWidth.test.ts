import { describe, expect, it } from 'vitest'
import { ref } from 'vue'
import { useMeasuredSelectWidth } from './useMeasuredSelectWidth'

describe('useMeasuredSelectWidth', () => {
  it('calculates width from measured text and extra control space', async () => {
    const label = ref('Longest option')
    const measured = useMeasuredSelectWidth(label, '--select-width')

    measured.measureEl.value = { scrollWidth: 180 } as HTMLElement
    await measured.updateWidth()

    expect(measured.widthStyle.value).toEqual({ '--select-width': '258px' })
  })
})
