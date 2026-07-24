<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/api/client'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

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
    errorMessage.value = '재설정 링크가 올바르지 않습니다.'
    return
  }
  if (newPassword.value !== newPasswordConfirm.value) {
    errorMessage.value = '비밀번호가 일치하지 않습니다.'
    return
  }

  status.value = 'submitting'
  try {
    await authStore.resetPassword(token.value, newPassword.value)
    status.value = 'success'
  } catch (error) {
    status.value = 'idle'
    errorMessage.value = getErrorMessage(error, '비밀번호 재설정에 실패했습니다.')
  }
}
</script>

<template>
  <section class="flex justify-center bg-soft-50 px-6 py-24">
    <div class="w-full max-w-md rounded-xl border border-slate-200 bg-white p-8">
      <template v-if="status === 'success'">
        <p class="text-center text-3xl">✅</p>
        <h1 class="mt-4 text-center text-lg font-bold text-navy-950">비밀번호가 재설정되었습니다</h1>
        <p class="mt-2 text-center text-sm text-slate-500">새 비밀번호로 다시 로그인해 주세요.</p>
        <button
          class="mt-6 w-full rounded-lg bg-navy-950 py-3 text-sm font-semibold text-white hover:bg-navy-900"
          @click="router.push('/auth?tab=login')"
        >
          로그인하러 가기
        </button>
      </template>

      <template v-else-if="!token">
        <p class="text-center text-3xl">⚠️</p>
        <h1 class="mt-4 text-center text-lg font-bold text-navy-950">재설정 링크가 올바르지 않습니다</h1>
        <RouterLink
          to="/forgot-password"
          class="mt-6 block rounded-lg bg-navy-950 py-3 text-center text-sm font-semibold text-white hover:bg-navy-900"
        >
          다시 요청하기
        </RouterLink>
      </template>

      <template v-else>
        <h1 class="text-lg font-bold text-navy-950">새 비밀번호 설정</h1>
        <p class="mt-2 text-sm text-slate-500">새로 사용할 비밀번호를 입력해 주세요.</p>

        <form class="mt-6 space-y-5" @submit.prevent="submit">
          <label class="block">
            <span class="text-sm font-medium text-navy-950">새 비밀번호</span>
            <input
              v-model="newPassword"
              type="password"
              placeholder="8자 이상, 영문+숫자 포함"
              minlength="8"
              class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
              required
            />
          </label>
          <label class="block">
            <span class="text-sm font-medium text-navy-950">새 비밀번호 확인</span>
            <input
              v-model="newPasswordConfirm"
              type="password"
              placeholder="비밀번호 재입력"
              class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
              required
            />
          </label>
          <button
            type="submit"
            :disabled="status === 'submitting'"
            class="w-full rounded-lg bg-navy-950 py-3 text-sm font-semibold text-white hover:bg-navy-900 disabled:opacity-50"
          >
            {{ status === 'submitting' ? '변경 중...' : '비밀번호 변경' }}
          </button>
        </form>

        <p v-if="errorMessage" class="mt-4 text-center text-sm text-red-600">{{ errorMessage }}</p>
      </template>
    </div>
  </section>
</template>
