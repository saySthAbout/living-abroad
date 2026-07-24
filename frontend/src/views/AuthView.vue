<script setup lang="ts">
import { ref, computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/api/client'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()

const activeTab = computed(() => (route.query.tab === 'signup' ? 'signup' : 'login'))

const loginForm = ref({ email: '', password: '' })
const signupForm = ref({ name: '', email: '', password: '', passwordConfirm: '' })
const errorMessage = ref('')

function switchTab(tab: 'login' | 'signup') {
  errorMessage.value = ''
  router.replace({ query: { tab } })
}

async function submitLogin() {
  errorMessage.value = ''
  try {
    await authStore.login(loginForm.value)
    router.push('/analysis/step-1')
  } catch (error) {
    errorMessage.value = getErrorMessage(error, t('auth.loginErrorFallback'))
  }
}

async function submitSignup() {
  errorMessage.value = ''
  if (signupForm.value.password !== signupForm.value.passwordConfirm) {
    errorMessage.value = t('auth.passwordMismatch')
    return
  }
  try {
    await authStore.signup({
      name: signupForm.value.name,
      email: signupForm.value.email,
      password: signupForm.value.password,
    })
    router.push('/analysis/step-1')
  } catch (error) {
    errorMessage.value = getErrorMessage(error, t('auth.signupErrorFallback'))
  }
}
</script>

<template>
  <section class="flex justify-center bg-soft-50 px-6 py-16">
    <div class="w-full max-w-md rounded-xl border border-slate-200 bg-white p-8">
      <div class="mb-8 flex border-b border-slate-200 text-center">
        <button
          class="flex-1 pb-3 text-sm font-semibold"
          :class="activeTab === 'login' ? 'border-b-2 border-navy-950 text-navy-950' : 'text-slate-400'"
          @click="switchTab('login')"
        >
          {{ t('auth.loginTab') }}
        </button>
        <button
          class="flex-1 pb-3 text-sm font-semibold"
          :class="activeTab === 'signup' ? 'border-b-2 border-navy-950 text-navy-950' : 'text-slate-400'"
          @click="switchTab('signup')"
        >
          {{ t('auth.signupTab') }}
        </button>
      </div>

      <form v-if="activeTab === 'login'" class="space-y-5" @submit.prevent="submitLogin">
        <label class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('auth.email') }}</span>
          <input
            v-model="loginForm.email"
            type="email"
            :placeholder="t('auth.emailPlaceholder')"
            class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
            required
          />
        </label>
        <label class="block">
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium text-navy-950">{{ t('auth.password') }}</span>
            <RouterLink to="/forgot-password" class="text-xs text-navy-700 hover:underline">{{ t('auth.forgotPassword') }}</RouterLink>
          </div>
          <input
            v-model="loginForm.password"
            type="password"
            :placeholder="t('auth.passwordPlaceholder')"
            class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
            required
          />
        </label>
        <button type="submit" class="w-full rounded-lg bg-navy-950 py-3 text-sm font-semibold text-white hover:bg-navy-900">
          {{ t('auth.loginButton') }}
        </button>
      </form>

      <form v-else class="space-y-5" @submit.prevent="submitSignup">
        <label class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('auth.name') }}</span>
          <input
            v-model="signupForm.name"
            type="text"
            :placeholder="t('auth.namePlaceholder')"
            class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
            required
          />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('auth.email') }}</span>
          <input
            v-model="signupForm.email"
            type="email"
            :placeholder="t('auth.emailPlaceholder')"
            class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
            required
          />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('auth.signupPassword') }}</span>
          <input
            v-model="signupForm.password"
            type="password"
            :placeholder="t('auth.signupPasswordPlaceholder')"
            minlength="8"
            class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
            required
          />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('auth.passwordConfirm') }}</span>
          <input
            v-model="signupForm.passwordConfirm"
            type="password"
            :placeholder="t('auth.passwordConfirmPlaceholder')"
            class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
            required
          />
        </label>
        <button type="submit" class="w-full rounded-lg bg-navy-950 py-3 text-sm font-semibold text-white hover:bg-navy-900">
          {{ t('auth.signupButton') }}
        </button>
      </form>

      <p v-if="errorMessage" class="mt-4 text-center text-sm text-red-600">{{ errorMessage }}</p>
      <p class="mt-4 text-center text-xs text-slate-400">{{ t('auth.footerNote') }}</p>
    </div>
  </section>
</template>
