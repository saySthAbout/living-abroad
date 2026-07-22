<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/api/client'

const authStore = useAuthStore()
const sending = ref(false)
const feedback = ref('')
const feedbackIsError = ref(false)

async function resend() {
  sending.value = true
  feedback.value = ''
  try {
    await authStore.resendVerificationEmail()
    feedbackIsError.value = false
    feedback.value = '인증 메일을 다시 보냈습니다. 받은편지함을 확인해 주세요.'
  } catch (error) {
    feedbackIsError.value = true
    feedback.value = getErrorMessage(error, '인증 메일 재전송에 실패했습니다. 잠시 후 다시 시도해 주세요.')
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div v-if="authStore.token && authStore.user && !authStore.user.emailVerified" class="border-b border-gold-600/30 bg-soft-100">
    <div class="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-6 py-2.5 text-sm">
      <p class="text-navy-950">
        ✉️ 이메일 인증이 완료되지 않았습니다.
        <span v-if="feedback" :class="feedbackIsError ? 'text-red-600' : 'text-navy-700'">{{ feedback }}</span>
      </p>
      <button
        class="shrink-0 rounded-lg bg-gold-500 px-3 py-1.5 text-xs font-semibold text-navy-950 hover:bg-gold-400 disabled:opacity-60"
        :disabled="sending"
        @click="resend"
      >
        {{ sending ? '전송 중...' : '인증 메일 재전송' }}
      </button>
    </div>
  </div>
</template>
