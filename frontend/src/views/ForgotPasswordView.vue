<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/api/client'

const authStore = useAuthStore()
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
    errorMessage.value = getErrorMessage(error, '요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.')
  }
}
</script>

<template>
  <section class="flex justify-center bg-soft-50 px-6 py-24">
    <div class="w-full max-w-md rounded-xl border border-slate-200 bg-white p-8">
      <template v-if="status === 'sent'">
        <p class="text-center text-3xl">📧</p>
        <h1 class="mt-4 text-center text-lg font-bold text-navy-950">이메일을 확인해 주세요</h1>
        <p class="mt-2 text-center text-sm text-slate-500">
          가입하신 이메일 주소로 계정이 존재하는 경우, 비밀번호 재설정 링크를 보내드렸습니다. 메일이 1시간 동안 유효합니다.
        </p>
        <RouterLink
          to="/auth"
          class="mt-6 block rounded-lg bg-navy-950 py-3 text-center text-sm font-semibold text-white hover:bg-navy-900"
        >
          로그인으로 돌아가기
        </RouterLink>
      </template>

      <template v-else>
        <h1 class="text-lg font-bold text-navy-950">비밀번호 찾기</h1>
        <p class="mt-2 text-sm text-slate-500">가입하신 이메일 주소를 입력하시면 비밀번호 재설정 링크를 보내드립니다.</p>

        <form class="mt-6 space-y-5" @submit.prevent="submit">
          <label class="block">
            <span class="text-sm font-medium text-navy-950">이메일 주소</span>
            <input
              v-model="email"
              type="email"
              placeholder="example@livingabroad.com"
              class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
              required
            />
          </label>
          <button
            type="submit"
            :disabled="status === 'submitting'"
            class="w-full rounded-lg bg-navy-950 py-3 text-sm font-semibold text-white hover:bg-navy-900 disabled:opacity-50"
          >
            {{ status === 'submitting' ? '전송 중...' : '재설정 링크 보내기' }}
          </button>
        </form>

        <p v-if="errorMessage" class="mt-4 text-center text-sm text-red-600">{{ errorMessage }}</p>
        <RouterLink to="/auth" class="mt-4 block text-center text-xs text-navy-700">로그인으로 돌아가기</RouterLink>
      </template>
    </div>
  </section>
</template>
