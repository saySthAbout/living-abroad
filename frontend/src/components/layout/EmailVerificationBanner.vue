<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/api/client'

const authStore = useAuthStore()
const { t } = useI18n()
const sending = ref(false)
const feedback = ref('')
const feedbackIsError = ref(false)

async function resend() {
  sending.value = true
  feedback.value = ''
  try {
    await authStore.resendVerificationEmail()
    feedbackIsError.value = false
    feedback.value = t('emailBanner.resendSuccess')
  } catch (error) {
    feedbackIsError.value = true
    feedback.value = getErrorMessage(error, t('emailBanner.resendErrorFallback'))
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div v-if="authStore.token && authStore.user && !authStore.user.emailVerified" class="border-b border-gold-600/30 bg-soft-100">
    <div class="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-6 py-2.5 text-sm">
      <p class="text-navy-950">
        {{ t('emailBanner.notVerified') }}
        <span v-if="feedback" :class="feedbackIsError ? 'text-red-600' : 'text-navy-700'">{{ feedback }}</span>
      </p>
      <button
        class="shrink-0 rounded-lg bg-gold-500 px-3 py-1.5 text-xs font-semibold text-navy-950 hover:bg-gold-400 disabled:opacity-60"
        :disabled="sending"
        @click="resend"
      >
        {{ sending ? t('emailBanner.resending') : t('emailBanner.resend') }}
      </button>
    </div>
  </div>
</template>
