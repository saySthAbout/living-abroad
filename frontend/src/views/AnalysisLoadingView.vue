<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAnalysisStore } from '@/stores/analysis'
import LoadingSpinner from '@/components/layout/LoadingSpinner.vue'

const router = useRouter()
const analysisStore = useAnalysisStore()
const errorMessage = ref('')
let pollTimer: ReturnType<typeof setInterval> | undefined

async function poll() {
  if (!analysisStore.analysisId) {
    errorMessage.value = '분석 요청을 찾을 수 없습니다.'
    return
  }
  try {
    const result = await analysisStore.loadResult(analysisStore.analysisId)
    if (result.status === 'COMPLETED') {
      clearInterval(pollTimer)
      router.push(`/results/${analysisStore.analysisId}`)
    } else if (result.status === 'FAILED') {
      clearInterval(pollTimer)
      errorMessage.value = '분석을 완료하지 못했습니다. 다시 시도해 주세요.'
    }
  } catch {
    clearInterval(pollTimer)
    errorMessage.value = '분석 상태를 확인하는 중 오류가 발생했습니다.'
  }
}

onMounted(() => {
  poll()
  pollTimer = setInterval(poll, 2500)
})

onUnmounted(() => {
  clearInterval(pollTimer)
})
</script>

<template>
  <section class="mx-auto max-w-md space-y-4 text-center">
    <h1 class="text-xl font-semibold">AI 분석 중</h1>
    <LoadingSpinner />
    <p class="text-sm text-gray-500">비자 규칙 · 국가 환경 · 경력 매칭을 분석하고 있습니다.</p>
    <p v-if="errorMessage" class="text-sm text-red-600">{{ errorMessage }}</p>
  </section>
</template>
