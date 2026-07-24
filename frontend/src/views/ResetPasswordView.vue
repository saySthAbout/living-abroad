<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/api/client'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()

const token = computed(() => {
  const value = route.query.token
  return typeof value === 'string' ? value : ''
})

const newPassword = ref('')
const newPasswordConfirm = ref('')
const status = ref<'idle' | 'submitting' | 'success'>('idle')
const errorMessage = ref('')

async function submit() {
  errorMessage.value = ''

  if (!token.value) {
    errorMessage.value = t('resetPassword.invalidLinkError')
    return
  }
  if (newPassword.value !== newPasswordConfirm.value) {
    errorMessage.value = t('auth.passwordMismatch')
    return
  }

  status.value = 'submitting'
  try {
    await authStore.resetPassword(token.value, newPassword.value)
    status.value = 'success'
  } catch (error) {
    status.value = 'idle'
    errorMessage.value = getErrorMessage(error, t('resetPassword.errorFallback'))
  }
}
</script>

<template>
  <section class="flex justify-center bg-soft-50 px-6 py-24">
    <div class="w-full max-w-md rounded-xl border border-slate-200 bg-white p-8">
      <template v-if="status === 'success'">
        <p class="text-center text-3xl">✅</p>
        <h1 class="mt-4 text-center text-lg font-bold text-navy-950">{{ t('resetPassword.successTitle') }}</h1>
        <p class="mt-2 text-center text-sm text-slate-500">{{ t('resetPassword.successDesc') }}</p>
        <button
          class="mt-6 w-full rounded-lg bg-navy-950 py-3 text-sm font-semibold text-white hover:bg-navy-900"
          @click="router.push('/auth?tab=login')"
        >
          {{ t('resetPassword.goLogin') }}
        </button>
      </template>

      <template v-else-if="!token">
        <p class="text-center text-3xl">⚠️</p>
        <h1 class="mt-4 text-center text-lg font-bold text-navy-950">{{ t('resetPassword.invalidTitle') }}</h1>
        <RouterLink
          to="/forgot-password"
          class="mt-6 block rounded-lg bg-navy-950 py-3 text-center text-sm font-semibold text-white hover:bg-navy-900"
        >
          {{ t('resetPassword.requestAgain') }}
        </RouterLink>
      </template>

      <template v-else>
        <h1 class="text-lg font-bold text-navy-950">{{ t('resetPassword.title') }}</h1>
        <p class="mt-2 text-sm text-slate-500">{{ t('resetPassword.desc') }}</p>

        <form class="mt-6 space-y-5" @submit.prevent="submit">
          <label class="block">
            <span class="text-sm font-medium text-navy-950">{{ t('resetPassword.newPassword') }}</span>
            <input
              v-model="newPassword"
              type="password"
              :placeholder="t('resetPassword.newPasswordPlaceholder')"
              minlength="8"
              class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
              required
            />
          </label>
          <label class="block">
            <span class="text-sm font-medium text-navy-950">{{ t('resetPassword.newPasswordConfirm') }}</span>
            <input
              v-model="newPasswordConfirm"
              type="password"
              :placeholder="t('resetPassword.newPasswordConfirmPlaceholder')"
              class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
              required
            />
          </label>
          <button
            type="submit"
            :disabled="status === 'submitting'"
            class="w-full rounded-lg bg-navy-950 py-3 text-sm font-semibold text-white hover:bg-navy-900 disabled:opacity-50"
          >
            {{ status === 'submitting' ? t('resetPassword.submitting') : t('resetPassword.submit') }}
          </button>
        </form>

        <p v-if="errorMessage" class="mt-4 text-center text-sm text-red-600">{{ errorMessage }}</p>
      </template>
    </div>
  </section>
</template>
