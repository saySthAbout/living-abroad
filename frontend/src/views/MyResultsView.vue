<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { apiClient, getErrorMessage } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import DisclaimerBox from '@/components/layout/DisclaimerBox.vue'
import LoadingSpinner from '@/components/layout/LoadingSpinner.vue'
import { COUNTRIES } from '@/constants/countries'

interface AnalysisHistoryItem {
  analysisId: number
  topCountryCode: 'CAN' | 'AUS' | 'GBR'
  topVisaName: string
  topScore: number
  analyzedAt: string
}

const authStore = useAuthStore()
const items = ref<AnalysisHistoryItem[]>([])
const loading = ref(true)
const loadError = ref('')

const latestAnalyzedAt = computed(() => items.value[0]?.analyzedAt?.slice(0, 10))

function flagFor(code: string) {
  return COUNTRIES.find((country) => country.code === code)?.flag ?? '🌐'
}

function countryNameFor(code: string) {
  return COUNTRIES.find((country) => country.code === code)?.nameKo ?? code
}

onMounted(async () => {
  try {
    const { data } = await apiClient.get('/api/analyses')
    items.value = data.items ?? []
  } catch (error) {
    loadError.value = getErrorMessage(error, '분석 이력을 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="mx-auto max-w-4xl px-6 py-10">
    <div class="rounded-xl bg-navy-950 px-6 py-8 text-white sm:px-10">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 class="text-2xl font-bold">안녕하세요, {{ authStore.user?.name ?? '회원' }}님</h1>
          <p v-if="latestAnalyzedAt" class="mt-3 text-sm text-white/70">📅 최근 분석일: {{ latestAnalyzedAt }}</p>
        </div>
        <RouterLink to="/analysis/step-1?mode=edit" class="rounded-lg bg-white/10 px-4 py-2 text-sm font-semibold hover:bg-white/20">
          ✎ 프로필 수정
        </RouterLink>
      </div>
    </div>

    <div class="mt-8 flex items-center justify-between">
      <h2 class="flex items-center gap-2 text-lg font-bold text-navy-950">↺ 최근 분석 기록</h2>
      <span class="text-sm text-slate-400">총 {{ items.length }}건의 분석 기록이 있습니다</span>
    </div>

    <LoadingSpinner v-if="loading" />
    <p v-else-if="loadError" class="mt-4 text-sm text-red-600">{{ loadError }}</p>
    <p v-else-if="items.length === 0" class="mt-4 rounded-xl border border-slate-200 px-6 py-10 text-center text-sm text-slate-500">
      저장된 분석 결과가 없습니다.
      <RouterLink to="/analysis/step-1" class="ml-1 font-semibold text-navy-950">지금 첫 분석을 시작해 보세요 →</RouterLink>
    </p>

    <div v-else class="mt-4 space-y-4">
      <div v-for="item in items" :key="item.analysisId" class="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-200 p-5">
        <div>
          <p class="text-xs text-slate-400">분석일: {{ item.analyzedAt?.slice(0, 10) }}</p>
          <p class="mt-1 font-semibold text-navy-950">
            {{ flagFor(item.topCountryCode) }} 추천 1순위: {{ countryNameFor(item.topCountryCode) }} ({{ item.topCountryCode }})
          </p>
          <p class="text-sm text-slate-500">대표 비자: {{ item.topVisaName }}</p>
        </div>
        <div class="flex items-center gap-6">
          <div class="text-right">
            <p class="text-2xl font-extrabold text-navy-950">
              {{ item.topScore }}<span class="text-sm font-medium text-slate-400"> / 100점</span>
            </p>
            <p class="text-[11px] tracking-wide text-slate-400">SUITABILITY SCORE</p>
          </div>
          <RouterLink :to="`/results/${item.analysisId}`" class="rounded-lg bg-navy-950 px-4 py-2.5 text-sm font-semibold text-white hover:bg-navy-900">
            결과 다시 보기 →
          </RouterLink>
        </div>
      </div>
    </div>

    <div class="mt-8">
      <DisclaimerBox
        :text="`적합도 점수는 실제 비자 승인 확률이 아닌 Living Abroad 플랫폼 내부의 데이터 분석 결과입니다. 본 결과는 참고용이며, 정확한 법적 자문은 전문 이민 변호사와 상담하시기 바랍니다.${latestAnalyzedAt ? ` 정보 기준일: ${latestAnalyzedAt} 기준.` : ''}`"
      />
    </div>
  </section>
</template>
