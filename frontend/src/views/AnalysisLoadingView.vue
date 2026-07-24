<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAnalysisStore } from '@/stores/analysis'
import DisclaimerBox from '@/components/layout/DisclaimerBox.vue'
import { getErrorMessage } from '@/api/client'

const router = useRouter()
const analysisStore = useAnalysisStore()
const { t } = useI18n()
const errorMessage = ref('')
const progress = ref(4)
let pollTimer: ReturnType<typeof setInterval> | undefined
let progressTimer: ReturnType<typeof setInterval> | undefined

const stages = computed(() => [
  { label: t('loading.stage1'), threshold: 20 },
  { label: t('loading.stage2'), threshold: 45 },
  { label: t('loading.stage3'), threshold: 75 },
  { label: t('loading.stage4'), threshold: 100 },
])

function stageState(threshold: number) {
  if (progress.value >= threshold) return 'done'
  if (progress.value >= threshold - 25) return 'active'
  return 'pending'
}

const overallLabel = computed(() => (progress.value >= 100 ? t('loading.complete') : t('loading.inProgress')))

async function poll() {
  if (!analysisStore.analysisId) {
    errorMessage.value = t('loading.notFoundError')
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
      errorMessage.value = t('loading.failedError')
    }
  } catch (error) {
    clearInterval(pollTimer)
    clearInterval(progressTimer)
    errorMessage.value = getErrorMessage(error, t('loading.statusCheckError'))
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
      <span class="flex items-center gap-1 text-navy-950">{{ t('loading.stepInputDone') }}</span>
      <div class="h-1 flex-1 overflow-hidden rounded-full bg-soft-100">
        <div class="h-full bg-navy-950 transition-all" :style="{ width: `${progress}%` }" />
      </div>
      <span class="text-navy-950">{{ t('loading.stepAnalyzing') }}</span>
      <span class="text-slate-300">{{ t('loading.stepResult') }}</span>
      <span class="ml-2 text-slate-400">{{ progress }}%</span>
    </div>

    <h1 class="text-center text-2xl font-bold text-navy-950">{{ t('loading.title') }}</h1>
    <p class="mt-2 text-center text-sm text-slate-500">
      {{ t('loading.subtitle') }}
    </p>

    <div class="mt-8 grid gap-6 rounded-xl border border-slate-200 p-6 sm:grid-cols-[auto_1fr] sm:p-8">
      <div class="flex flex-col items-center justify-center rounded-lg border-2 border-navy-950 px-8 py-6">
        <span class="text-3xl font-extrabold text-navy-950">{{ progress }}%</span>
        <span class="mt-1 text-xs font-semibold tracking-wide text-slate-400">{{ t('loading.processing') }}</span>
      </div>

      <div>
        <div class="flex items-center justify-between text-xs font-semibold uppercase tracking-wide text-slate-400">
          <span>{{ t('loading.analysisStatus') }}</span>
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
        {{ t('loading.cancel') }}
      </button>
      <button class="rounded-lg bg-soft-100 px-5 py-2.5 text-sm font-semibold text-slate-400" disabled>
        {{ t('loading.generating') }}
      </button>
    </div>

    <p v-if="errorMessage" class="mt-4 text-center text-sm text-red-600">{{ errorMessage }}</p>

    <div class="mt-8">
      <DisclaimerBox :text="t('loading.disclaimer')" />
    </div>
  </section>
</template>
