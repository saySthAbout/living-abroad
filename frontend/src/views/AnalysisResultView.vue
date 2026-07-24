<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAnalysisStore } from '@/stores/analysis'
import { useAuthStore } from '@/stores/auth'
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
  ruleScore: number
  environmentScore: number
  careerSimilarity: number
  preferenceScore: number
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
const router = useRouter()
const analysisStore = useAnalysisStore()
const authStore = useAuthStore()
const loading = ref(true)
const loadError = ref('')

const checklistItems = [
  '유효한 여권',
  '공인 영어 성적표(IELTS/PTE 등)',
  '학력 인증서(ECA 또는 국가별 인증)',
  '영문 경력증명서',
  '자금 증빙서류',
]
const checkedItems = ref<boolean[]>(checklistItems.map(() => false))

const result = computed(() => analysisStore.analysisResult as unknown as AnalysisDetail | null)
const sortedResults = computed(() => [...(result.value?.results ?? [])].sort((a, b) => a.rank - b.rank))

const shareStatus = ref<'idle' | 'creating' | 'ready' | 'error'>('idle')
const shareUrl = ref('')
const shareErrorMessage = ref('')
const copyLabel = ref('링크 복사')

function flagFor(code: string) {
  return COUNTRIES.find((country) => country.code === code)?.flag ?? '🌐'
}

function exportPdf() {
  window.print()
}

async function createShare() {
  shareStatus.value = 'creating'
  shareErrorMessage.value = ''
  try {
    const token = await analysisStore.createShareLink(Number(route.params.id))
    shareUrl.value = `${window.location.origin}/shared/${token}`
    shareStatus.value = 'ready'
  } catch (error) {
    shareStatus.value = 'error'
    shareErrorMessage.value = getErrorMessage(error, '공유 링크 생성에 실패했습니다.')
  }
}

async function copyShareUrl() {
  await navigator.clipboard.writeText(shareUrl.value)
  copyLabel.value = '복사됨!'
  setTimeout(() => {
    copyLabel.value = '링크 복사'
  }, 2000)
}

onMounted(async () => {
  const analysisId = Number(route.params.id)
  try {
    await analysisStore.loadResult(analysisId)
  } catch (error) {
    loadError.value = getErrorMessage(error, '분석 결과를 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="mx-auto max-w-6xl px-6 py-10">
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <h1 class="flex items-center gap-2 text-2xl font-bold text-navy-950">📊 AI 맞춤 이민 분석 결과</h1>
        <p class="mt-2 text-sm text-slate-500">
          {{ authStore.user?.name ?? '회원' }} 님
          <span v-if="result">· 분석일: {{ result.analyzedAt?.slice(0, 10) }}</span>
        </p>
      </div>
      <span class="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-500">
        ⓘ 서비스 내부 적합도 점수 안내
      </span>
    </div>

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

          <div class="mt-4 space-y-2 text-xs">
            <div>
              <div class="flex justify-between text-slate-500"><span>국가 환경 점수</span><span>{{ item.environmentScore }}/100</span></div>
              <div class="mt-1 h-1.5 rounded-full bg-soft-100">
                <div class="h-full rounded-full bg-navy-700" :style="{ width: `${item.environmentScore}%` }" />
              </div>
            </div>
            <div>
              <div class="flex justify-between text-slate-500"><span>경력·직업 유사도</span><span>{{ item.careerSimilarity }}/100</span></div>
              <div class="mt-1 h-1.5 rounded-full bg-soft-100">
                <div class="h-full rounded-full bg-navy-700" :style="{ width: `${item.careerSimilarity}%` }" />
              </div>
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

      <h2 class="mt-10 flex items-center gap-2 text-lg font-bold text-navy-950">↗ 국가별 종합 비교</h2>
      <div class="mt-4 overflow-x-auto rounded-xl border border-slate-200">
        <table class="w-full text-left text-sm">
          <thead class="bg-soft-50 text-slate-500">
            <tr>
              <th class="px-4 py-3 font-medium">분석 지표</th>
              <th v-for="item in sortedResults" :key="item.countryCode" class="px-4 py-3 font-medium">{{ item.countryName }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr>
              <td class="px-4 py-3 font-medium text-navy-950">종합 적합도</td>
              <td v-for="item in sortedResults" :key="item.countryCode" class="px-4 py-3 font-bold text-navy-950">{{ item.totalScore }}점</td>
            </tr>
            <tr>
              <td class="px-4 py-3 font-medium text-navy-950">국가 환경 점수</td>
              <td v-for="item in sortedResults" :key="item.countryCode" class="px-4 py-3 text-slate-600">{{ item.environmentScore }}점</td>
            </tr>
            <tr>
              <td class="px-4 py-3 font-medium text-navy-950">경력·직업 유사도</td>
              <td v-for="item in sortedResults" :key="item.countryCode" class="px-4 py-3 text-slate-600">{{ item.careerSimilarity }}점</td>
            </tr>
          </tbody>
        </table>
      </div>

      <h2 class="mt-10 flex items-center gap-2 text-lg font-bold text-navy-950">📋 준비 체크리스트</h2>
      <div class="mt-4 grid gap-3 sm:grid-cols-2">
        <label v-for="(item, index) in checklistItems" :key="item" class="flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm">
          <input v-model="checkedItems[index]" type="checkbox" class="h-4 w-4 accent-navy-950" />
          {{ item }}
        </label>
      </div>

      <div class="mt-8 flex flex-wrap items-center justify-between gap-4 border-t border-slate-100 pt-6">
        <DisclaimerBox :text="result.disclaimer ?? '본 점수는 실제 비자 승인 확률이 아닙니다.'" />
        <div class="flex shrink-0 flex-wrap gap-3 print:hidden">
          <button class="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-600" @click="exportPdf">
            🖨 PDF로 저장
          </button>
          <button class="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-600" @click="createShare">
            🔗 공유하기
          </button>
          <button class="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-600" @click="router.push('/analysis/step-1')">
            ↻ 다시 분석하기
          </button>
          <button class="rounded-lg bg-navy-950 px-4 py-2.5 text-sm font-semibold text-white hover:bg-navy-900" @click="router.push('/chat')">
            💬 AI에게 결과 질문하기
          </button>
        </div>
      </div>

      <div v-if="shareStatus !== 'idle'" class="mt-4 rounded-lg border border-slate-200 bg-soft-50 p-4 text-sm print:hidden">
        <template v-if="shareStatus === 'creating'">공유 링크를 만드는 중...</template>
        <template v-else-if="shareStatus === 'ready'">
          <p class="text-slate-500">이 링크가 있는 사람은 로그인 없이 이 결과를 볼 수 있습니다.</p>
          <div class="mt-2 flex flex-wrap items-center gap-2">
            <input :value="shareUrl" readonly class="min-w-0 flex-1 rounded-lg border border-slate-300 px-3 py-2 text-xs text-slate-600" />
            <button class="shrink-0 rounded-lg bg-navy-950 px-3 py-2 text-xs font-semibold text-white hover:bg-navy-900" @click="copyShareUrl">
              {{ copyLabel }}
            </button>
          </div>
        </template>
        <template v-else>
          <p class="text-red-600">{{ shareErrorMessage }}</p>
        </template>
      </div>
    </template>
  </section>
</template>
