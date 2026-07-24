<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/api/client'

const authStore = useAuthStore()
const { t } = useI18n()
const email = ref('')
const status = ref<'idle' | 'submitting' | 'sent'>('idle')
const errorMessage = ref('')

async function submit() {
  errorMessage.value = ''
  status.value = 'submitting'
  try {
    await authStore.forgotPassword(email.value)
    status.value = 'sent'
  } catch (error) {
    status.value = 'idle'
    errorMessage.value = getErrorMessage(error, t('forgotPassword.errorFallback'))
  }
}
</script>

<template>
  <section class="flex justify-center bg-soft-50 px-6 py-24">
    <div class="w-full max-w-md rounded-xl border border-slate-200 bg-white p-8">
      <template v-if="status === 'sent'">
        <p class="text-center text-3xl">📧</p>
        <h1 class="mt-4 text-center text-lg font-bold text-navy-950">{{ t('forgotPassword.checkEmailTitle') }}</h1>
        <p class="mt-2 text-center text-sm text-slate-500">
          {{ t('forgotPassword.checkEmailDesc') }}
        </p>
        <RouterLink
          to="/auth"
          class="mt-6 block rounded-lg bg-navy-950 py-3 text-center text-sm font-semibold text-white hover:bg-navy-900"
        >
          {{ t('forgotPassword.backToLogin') }}
        </RouterLink>
      </template>

      <template v-else>
        <h1 class="text-lg font-bold text-navy-950">{{ t('forgotPassword.title') }}</h1>
        <p class="mt-2 text-sm text-slate-500">{{ t('forgotPassword.desc') }}</p>

        <form class="mt-6 space-y-5" @submit.prevent="submit">
          <label class="block">
            <span class="text-sm font-medium text-navy-950">{{ t('auth.email') }}</span>
            <input
              v-model="email"
              type="email"
              :placeholder="t('auth.emailPlaceholder')"
              class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
              required
            />
          </label>
          <button
            type="submit"
            :disabled="status === 'submitting'"
            class="w-full rounded-lg bg-navy-950 py-3 text-sm font-semibold text-white hover:bg-navy-900 disabled:opacity-50"
          >
            {{ status === 'submitting' ? t('forgotPassword.submitting') : t('forgotPassword.submit') }}
          </button>
        </form>

        <p v-if="errorMessage" class="mt-4 text-center text-sm text-red-600">{{ errorMessage }}</p>
        <RouterLink to="/auth" class="mt-4 block text-center text-xs text-navy-700">{{ t('forgotPassword.backToLogin') }}</RouterLink>
      </template>
    </div>
  </section>
</template>
