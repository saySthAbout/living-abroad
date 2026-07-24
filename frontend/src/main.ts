import { createApp } from 'vue'
import { createPinia } from 'pinia'
import * as Sentry from '@sentry/vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import { useAuthStore } from '@/stores/auth'

document.documentElement.lang = i18n.global.locale.value

const app = createApp(App)

if (import.meta.env.VITE_SENTRY_DSN) {
  Sentry.init({
    app,
    dsn: import.meta.env.VITE_SENTRY_DSN,
    environment: import.meta.env.VITE_SENTRY_ENVIRONMENT ?? 'local',
  })
}

app.use(createPinia())
app.use(router)
app.use(i18n)

useAuthStore().loadUser()

app.mount('#app')
