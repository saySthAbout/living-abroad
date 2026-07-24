<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { setLocale } from '@/i18n'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { t, locale } = useI18n()

const isLoginTabActive = computed(() => route.name === 'AUTH' && route.query.tab !== 'signup')

function handleLogout() {
  authStore.logout()
  router.push('/')
}

function toggleLocale() {
  setLocale(locale.value === 'ko' ? 'en' : 'ko')
}
</script>

<template>
  <header class="border-b border-slate-200 bg-white">
    <div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
      <RouterLink to="/" class="text-lg font-bold text-navy-950">Living Abroad</RouterLink>
      <nav class="hidden items-center gap-8 text-sm font-medium text-slate-500 sm:flex">
        <RouterLink to="/" class="hover:text-navy-950 [&.router-link-exact-active]:font-semibold [&.router-link-exact-active]:text-navy-950">{{ t('header.home') }}</RouterLink>
        <RouterLink to="/analysis/step-1" class="hover:text-navy-950 [&.router-link-active]:font-semibold [&.router-link-active]:text-navy-950">{{ t('header.aiAnalysis') }}</RouterLink>
        <RouterLink to="/chat" class="hover:text-navy-950 [&.router-link-active]:font-semibold [&.router-link-active]:text-navy-950">{{ t('header.aiConsult') }}</RouterLink>
        <RouterLink to="/my-results" class="hover:text-navy-950 [&.router-link-active]:font-semibold [&.router-link-active]:text-navy-950">{{ t('header.myResults') }}</RouterLink>
      </nav>
      <div class="flex items-center gap-3">
        <button
          type="button"
          class="rounded border border-slate-300 px-2 py-1 text-xs font-semibold text-slate-500 hover:text-navy-950"
          @click="toggleLocale"
        >
          {{ locale === 'ko' ? 'EN' : '한국어' }}
        </button>
        <template v-if="!authStore.token">
          <RouterLink
            to="/auth?tab=login"
            :class="isLoginTabActive ? 'rounded-lg bg-navy-950 px-4 py-2 text-sm font-semibold text-white hover:bg-navy-900' : 'text-sm font-medium text-slate-600 hover:text-navy-950'"
          >
            {{ t('header.login') }}
          </RouterLink>
          <RouterLink
            to="/auth?tab=signup"
            :class="isLoginTabActive ? 'text-sm font-medium text-slate-600 hover:text-navy-950' : 'rounded-lg bg-navy-950 px-4 py-2 text-sm font-semibold text-white hover:bg-navy-900'"
          >
            {{ t('header.signup') }}
          </RouterLink>
        </template>
        <button
          v-else
          class="text-sm font-medium text-slate-600 hover:text-navy-950"
          @click="handleLogout"
        >
          {{ t('header.logout') }}
        </button>
      </div>
    </div>
  </header>
</template>
