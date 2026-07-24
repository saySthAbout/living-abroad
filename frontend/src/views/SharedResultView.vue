<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAnalysisStore } from '@/stores/analysis'
import DisclaimerBox from '@/components/layout/DisclaimerBox.vue'
import LoadingSpinner from '@/components/layout/LoadingSpinner.vue'
import { COUNTRIES } from '@/constants/countries'
import { getErrorMessage } from '@/api/client'

interface CountryResult {
  rank: number
  countryCode: 'CAN' | 'AUS' | 'GBR'
  countryName: string
  visaCode: string
  visaName: string
  totalScore: number
  environmentScore: number
  careerSimilarity: number
  strengths?: string[]
  improvements?: string[]
}

interface AnalysisDetail {
  analysisId: number
  analyzedAt: string
  results: CountryResult[]
  disclaimer?: string
}

const route = useRoute()
const analysisStore = useAnalysisStore()
const loading = ref(true)
const loadError = ref('')
const result = ref<AnalysisDetail | null>(null)

const sortedResults = computed(() => [...(result.value?.results ?? [])].sort((a, b) => a.rank - b.rank))

function flagFor(code: string) {
  return COUNTRIES.find((country) => country.code === code)?.flag ?? '🌐'
}

onMounted(async () => {
  const token = route.params.token
  if (typeof token !== 'string' || !token) {
    loadError.value = '공유 링크가 올바르지 않습니다.'
    loading.value = false
    return
  }
  try {
    result.value = (await analysisStore.loadSharedResult(token)) as unknown as AnalysisDetail
  } catch (error) {
    loadError.value = getErrorMessage(error, '공유 링크를 찾을 수 없거나 더 이상 유효하지 않습니다.')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="mx-auto max-w-6xl px-6 py-10">
    <div class="rounded-lg border border-navy-100 bg-soft-50 px-4 py-3 text-sm text-navy-700">
      🔗 다른 사람이 공유한 Living Abroad 분석 결과입니다. 로그인 없이 조회 중입니다.
    </div>

    <h1 class="mt-6 flex items-center gap-2 text-2xl font-bold text-navy-950">📊 AI 맞춤 이민 분석 결과</h1>

    <LoadingSpinner v-if="loading" />
    <p v-else-if="loadError" class="mt-8 text-sm text-red-600">{{ loadError }}</p>

    <template v-else-if="result">
      <h2 class="mt-8 text-lg font-bold text-navy-950">★ 추천 국가 TOP 3</h2>
      <div class="mt-4 grid gap-6 lg:grid-cols-3">
        <div
          v-for="item in sortedResults"
          :key="item.countryCode"
          class="rounded-xl border-2 p-5"
          :class="item.rank === 1 ? 'border-gold-500' : 'border-slate-200'"
        >
          <span v-if="item.rank === 1" class="mb-3 inline-block rounded bg-gold-500 px-2 py-1 text-xs font-bold text-navy-950">
            BEST MATCH
          </span>
          <h3 class="text-lg font-bold text-navy-950">{{ flagFor(item.countryCode) }} {{ item.countryName }}</h3>

          <div class="mt-3 flex items-end justify-between">
            <div class="text-xs text-slate-400">
              대표 비자<br />
              <span class="text-sm font-medium text-navy-950">{{ item.visaName }}</span>
            </div>
            <div class="text-right">
              <span class="text-3xl font-extrabold text-navy-950">{{ item.totalScore }}</span>
              <p class="text-xs text-slate-400">종합 적합도</p>
            </div>
          </div>

          <div class="mt-4 flex flex-wrap gap-2">
            <span v-for="strength in item.strengths ?? []" :key="strength" class="rounded-full bg-emerald-50 px-2 py-1 text-xs text-emerald-700">
              + {{ strength }}
            </span>
            <span v-for="improvement in item.improvements ?? []" :key="improvement" class="rounded-full bg-amber-50 px-2 py-1 text-xs text-amber-700">
              - {{ improvement }}
            </span>
          </div>
        </div>
      </div>

      <div class="mt-8 flex flex-wrap items-center justify-between gap-4 border-t border-slate-100 pt-6">
        <DisclaimerBox :text="result.disclaimer ?? '본 점수는 실제 비자 승인 확률이 아닙니다.'" />
        <RouterLink to="/" class="shrink-0 rounded-lg bg-navy-950 px-4 py-2.5 text-sm font-semibold text-white hover:bg-navy-900">
          나도 분석해보기
        </RouterLink>
      </div>
    </template>
  </section>
</template>
