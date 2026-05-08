import { en } from './messages/en'
import { ja } from './messages/ja'

export const messages = {
  en,
  ja
} as const

export type MessageSchema = typeof en
