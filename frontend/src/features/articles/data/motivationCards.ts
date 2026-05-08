import type { MotivationCardData } from '../types'
import { enMotivationMessages, jaMotivationMessages, type MotivationMessage } from './motivationMessages'
import { motivationIllustrations, motivationPalettes } from './motivationVisuals'

export const motivationCards: MotivationCardData[] = toCards(jaMotivationMessages)

export function getMotivationCards(locale: string): MotivationCardData[] {
  return locale === 'ja' ? motivationCards : toCards(enMotivationMessages)
}

function toCards(source: readonly MotivationMessage[]): MotivationCardData[] {
  return source.map(([title, note], index) => {
    const [background, accent, ink] = motivationPalettes[index % motivationPalettes.length]

    return {
      id: index + 1,
      title,
      note,
      illustration: motivationIllustrations[index % motivationIllustrations.length],
      background,
      accent,
      ink
    }
  })
}
