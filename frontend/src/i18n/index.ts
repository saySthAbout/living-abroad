import { createI18n } from 'vue-i18n'
import ko from './locales/ko'
import en from './locales/en'

export type SupportedLocale = 'ko' | 'en'

const STORAGE_KEY = 'locale'

function resolveInitialLocale(): SupportedLocale {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored === 'ko' || stored === 'en') {
    return stored
  }
  return navigator.language.toLowerCase().startsWith('en') ? 'en' : 'ko'
}

export const i18n = createI18n({
  legacy: false,
  locale: resolveInitialLocale(),
  fallbackLocale: 'ko',
  messages: { ko, en },
})

export function setLocale(locale: SupportedLocale) {
  i18n.global.locale.value = locale
  localStorage.setItem(STORAGE_KEY, locale)
  document.documentElement.lang = locale
}
