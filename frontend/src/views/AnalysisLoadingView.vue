<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAnalysisStore } from '@/stores/analysis'
import DisclaimerBox from '@/components/layout/DisclaimerBox.vue'

const router = useRouter()
const analysisStore = useAnalysisStore()
const errorMessage = ref('')
const progress = ref(4)
let pollTimer: ReturnType<typeof setInterval> | undefined
let progressTimer: ReturnType<typeof setInterval> | undefined

const stages = [
  { label: '기본 자격 요건 확인', threshold: 20 },
  { label: '경력·직업군 매칭', threshold: 45 },
  { label: '국가별 환경 데이터 분석', threshold: 75 },
  { label: '추천 결과 생성', threshold: 100 },
]

function stageState(threshold: number) {
  if (progress.value >= threshold) return 'done'
  if (progress.value >= threshold - 25) return 'active'
  return 'pending'
}

const overallLabel = computed(() => (progress.value >= 100 ? '완료' : '진행 중'))

async function poll() {
  if (!analysisStore.analysisId) {
    errorMessage.value = '분석 요청을 찾을 수 없습니다.'
    return
  }
  try {
    const result = await analysisStore.loadResult(analysisStore.analysisId)
    if (result.status === 'COMPLETED') {
      progress.value = 100
      clearInterval(pollTimer)
      clearInterval(progressTimer)
      router.push(`/results/${analysisStore.analysisId}`)
    } else if (result.status === 'FAILED') {
      clearInterval(pollTimer)
      clearInterval(progressTimer)
      errorMessage.value = '분석을 완료하지 못했습니다. 다시 시도해 주세요.'
    }
  } catch {
    clearInterval(pollTimer)
    clearInterval(progressTimer)
    errorMessage.value = '분석 상태를 확인하는 중 오류가 발생했습니다.'
  }
}

function cancelAnalysis() {
  clearInterval(pollTimer)
  clearInterval(progressTimer)
  router.push('/analysis/step-2')
}

onMounted(() => {
  poll()
  pollTimer = setInterval(poll, 2500)
  progressTimer = setInterval(() => {
    if (progress.value < 92) progress.value += 4
  }, 600)
})

onUnmounted(() => {
  clearInterval(pollTimer)
  clearInterval(progressTimer)
})
</script>

<template>
  <section class="mx-auto max-w-3xl px-6 py-12">
    <div class="mb-8 flex items-center gap-3 text-sm font-medium">
      <span class="flex items-center gap-1 text-navy-950">✓ 입력 완료</span>
      <div class="h-1 flex-1 overflow-hidden rounded-full bg-soft-100">
        <div class="h-full bg-navy-950 transition-all" :style="{ width: `${progress}%` }" />
      </div>
      <span class="text-navy-950">AI 분석 중</span>
      <span class="text-slate-300">추천 결과</span>
      <span class="ml-2 text-slate-400">{{ progress }}%</span>
    </div>

    <h1 class="text-center text-2xl font-bold text-navy-950">입력하신 정보를 분석하고 있습니다.</h1>
    <p class="mt-2 text-center text-sm text-slate-500">
      국가별 자격 요건, 경력·직업군 유사도와 정착 환경 데이터를 비교하고 있습니다.
    </p>

    <div class="mt-8 grid gap-6 rounded-xl border border-slate-200 p-6 sm:grid-cols-[auto_1fr] sm:p-8">
      <div class="flex flex-col items-center justify-center rounded-lg border-2 border-navy-950 px-8 py-6">
        <span class="text-3xl font-extrabold text-navy-950">{{ progress }}%</span>
        <span class="mt-1 text-xs font-semibold tracking-wide text-slate-400">PROCESSING</span>
      </div>

      <div>
        <div class="flex items-center justify-between text-xs font-semibold uppercase tracking-wide text-slate-400">
          <span>Analysis Status</span>
          <span class="text-navy-950">{{ overallLabel }}</span>
        </div>
        <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-soft-100">
          <div class="h-full bg-navy-950 transition-all" :style="{ width: `${progress}%` }" />
        </div>

        <ul class="mt-4 space-y-2 text-sm">
          <li v-for="stage in stages" :key="stage.label" class="flex items-center gap-2">
            <span v-if="stageState(stage.threshold) === 'done'" class="text-emerald-600">✔</span>
            <span v-else-if="stageState(stage.threshold) === 'active'" class="animate-spin text-navy-700">↻</span>
            <span v-else class="text-slate-300">○</span>
            <span :class="stageState(stage.threshold) === 'pending' ? 'text-slate-400' : 'text-navy-950'">{{ stage.label }}</span>
          </li>
        </ul>
      </div>
    </div>

    <div class="mt-6 flex justify-center gap-3">
      <button class="rounded-lg border border-slate-300 px-5 py-2.5 text-sm font-semibold text-slate-600" @click="cancelAnalysis">
        ✕ 분석 취소
      </button>
      <button class="rounded-lg bg-soft-100 px-5 py-2.5 text-sm font-semibold text-slate-400" disabled>
        ⟳ 결과 산출 중...
      </button>
    </div>

    <p v-if="errorMessage" class="mt-4 text-center text-sm text-red-600">{{ errorMessage }}</p>

    <div class="mt-8">
      <DisclaimerBox text="본 분석은 Living Abroad의 내부 데이터를 기반으로 한 추정치이며, 실제 비자 승인을 보장하지 않습니다. 정보 기준일: YYYY.MM.DD" />
    </div>
  </section>
</template>
