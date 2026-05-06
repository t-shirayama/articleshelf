import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
import App from './App.vue'
import './styles.css'

const vuetify = createVuetify({
  components,
  directives,
  theme: {
    defaultTheme: 'readstack',
    themes: {
      readstack: {
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

createApp(App).use(createPinia()).use(vuetify).mount('#app')
