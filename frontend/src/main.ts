import { createApp } from 'vue'
import App from './App.vue'
import { i18n, pinia, router, vuetify } from './app/providers'
import './styles.css'

createApp(App).use(pinia).use(i18n).use(vuetify).use(router).mount('#app')
