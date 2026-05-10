import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { en, ja } from 'vuetify/locale'
import 'vuetify/styles'
import { getCurrentLocale } from '../../shared/i18n'

export const vuetify = createVuetify({
  components,
  directives,
  locale: {
    locale: getCurrentLocale(),
    fallback: 'en',
    messages: { ja, en }
  },
  theme: {
    defaultTheme: 'articleshelf',
    themes: {
      articleshelf: {
        dark: false,
        colors: {
          primary: '#186c74',
          secondary: '#d8efee',
          surface: '#fffdfa',
          background: '#f5f3ed',
          error: '#b33b2f',
          success: '#26744e',
          warning: '#94640c'
        }
      }
    }
  },
  defaults: {
    VBtn: {
      rounded: 'lg'
    },
    VCard: {
      rounded: 'lg'
    },
    VTextField: {
      density: 'comfortable',
      variant: 'outlined'
    },
    VTextarea: {
      density: 'comfortable',
      variant: 'outlined'
    },
    VSelect: {
      density: 'comfortable',
      variant: 'outlined'
    }
  }
})
