import type { MotivationCardData } from '../types'
import { enMotivationMessages, jaMotivationMessages, type MotivationMessage } from './motivationMessages'
import { motivationIllustrations, motivationPaletteClasses } from './motivationVisuals'

export const motivationCards: MotivationCardData[] = toCards(jaMotivationMessages)

export function getMotivationCards(locale: string): MotivationCardData[] {
  return locale === 'ja' ? motivationCards : toCards(enMotivationMessages)
}

function toCards(source: readonly MotivationMessage[]): MotivationCardData[] {
  return source.map(([title, note], index) => {
    return {
      id: index + 1,
      title,
      note,
      illustration: motivationIllustrations[index % motivationIllustrations.length],
      paletteClass: motivationPaletteClasses[index % motivationPaletteClasses.length]
    }
  })
}
