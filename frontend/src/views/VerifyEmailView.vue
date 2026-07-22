<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { apiClient, getErrorMessage } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import LoadingSpinner from '@/components/layout/LoadingSpinner.vue'

const route = useRoute()
const authStore = useAuthStore()

const status = ref<'loading' | 'success' | 'error'>('loading')
const errorMessage = ref('')

onMounted(async () => {
  const token = route.query.token
  if (typeof token !== 'string' || !token) {
    status.value = 'error'
    errorMessage.value = '인증 링크가 올바르지 않습니다.'
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
    errorMessage.value = getErrorMessage(error, '인증 링크가 유효하지 않거나 만료되었습니다.')
  }
})
</script>

<template>
  <section class="flex justify-center bg-soft-50 px-6 py-24">
    <div class="w-full max-w-md rounded-xl border border-slate-200 bg-white p-8 text-center">
      <template v-if="status === 'loading'">
        <LoadingSpinner />
        <p class="mt-4 text-sm text-slate-500">이메일 인증을 확인하는 중입니다...</p>
      </template>

      <template v-else-if="status === 'success'">
        <p class="text-3xl">✅</p>
        <h1 class="mt-4 text-lg font-bold text-navy-950">이메일 인증이 완료되었습니다</h1>
        <p class="mt-2 text-sm text-slate-500">이제 Living Abroad의 모든 기능을 이용하실 수 있습니다.</p>
        <RouterLink to="/" class="mt-6 inline-block rounded-lg bg-navy-950 px-6 py-3 text-sm font-semibold text-white hover:bg-navy-900">
          홈으로 이동
        </RouterLink>
      </template>

      <template v-else>
        <p class="text-3xl">⚠️</p>
        <h1 class="mt-4 text-lg font-bold text-navy-950">인증에 실패했습니다</h1>
        <p class="mt-2 text-sm text-slate-500">{{ errorMessage }}</p>
        <RouterLink to="/" class="mt-6 inline-block rounded-lg bg-navy-950 px-6 py-3 text-sm font-semibold text-white hover:bg-navy-900">
          홈으로 이동
        </RouterLink>
      </template>
    </div>
  </section>
</template>
