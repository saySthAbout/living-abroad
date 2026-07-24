<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { apiClient, getErrorMessage } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import LoadingSpinner from '@/components/layout/LoadingSpinner.vue'

const route = useRoute()
const authStore = useAuthStore()
const { t } = useI18n()

const status = ref<'loading' | 'success' | 'error'>('loading')
const errorMessage = ref('')

onMounted(async () => {
  const token = route.query.token
  if (typeof token !== 'string' || !token) {
    status.value = 'error'
    errorMessage.value = t('verifyEmail.invalidLink')
    return
  }

  try {
    await apiClient.post('/api/auth/verify-email', { token })
    status.value = 'success'
    if (authStore.user) {
      authStore.user.emailVerified = true
    }
  } catch (error) {
    status.value = 'error'
    errorMessage.value = getErrorMessage(error, t('verifyEmail.errorFallback'))
  }
})
</script>

<template>
  <section class="flex justify-center bg-soft-50 px-6 py-24">
    <div class="w-full max-w-md rounded-xl border border-slate-200 bg-white p-8 text-center">
      <template v-if="status === 'loading'">
        <LoadingSpinner />
        <p class="mt-4 text-sm text-slate-500">{{ t('verifyEmail.checking') }}</p>
      </template>

      <template v-else-if="status === 'success'">
        <p class="text-3xl">✅</p>
        <h1 class="mt-4 text-lg font-bold text-navy-950">{{ t('verifyEmail.successTitle') }}</h1>
        <p class="mt-2 text-sm text-slate-500">{{ t('verifyEmail.successDesc') }}</p>
        <RouterLink to="/" class="mt-6 inline-block rounded-lg bg-navy-950 px-6 py-3 text-sm font-semibold text-white hover:bg-navy-900">
          {{ t('verifyEmail.goHome') }}
        </RouterLink>
      </template>

      <template v-else>
        <p class="text-3xl">⚠️</p>
        <h1 class="mt-4 text-lg font-bold text-navy-950">{{ t('verifyEmail.failTitle') }}</h1>
        <p class="mt-2 text-sm text-slate-500">{{ errorMessage }}</p>
        <RouterLink to="/" class="mt-6 inline-block rounded-lg bg-navy-950 px-6 py-3 text-sm font-semibold text-white hover:bg-navy-900">
          {{ t('verifyEmail.goHome') }}
        </RouterLink>
      </template>
    </div>
  </section>
</template>
