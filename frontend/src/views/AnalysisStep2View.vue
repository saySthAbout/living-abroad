<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAnalysisStore } from '@/stores/analysis'

const router = useRouter()
const analysisStore = useAnalysisStore()

const form = reactive({ ...analysisStore.step2Data })
const errorMessage = ref('')
const submitting = ref(false)

const careerTextLength = computed(() => form.careerText.length)

const interestCountries = [
  { code: 'CAN', label: '캐나다', icon: '🇨🇦' },
  { code: 'AUS', label: '호주', icon: '🇦🇺' },
  { code: 'GBR', label: '영국', icon: '🇬🇧' },
  { code: 'ANY', label: '상관없음', icon: '🌐' },
] as const

function goPrev() {
  analysisStore.saveStep2({ ...form })
  router.push('/analysis/step-1')
}

async function submit() {
  errorMessage.value = ''
  analysisStore.saveStep2({ ...form })
  submitting.value = true
  try {
    await analysisStore.submitAnalysis()
    router.push('/analysis/loading')
  } catch {
    errorMessage.value = '분석 요청 중 오류가 발생했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="mx-auto max-w-3xl px-6 py-12">
    <div class="mb-10 flex items-center justify-center gap-4">
      <div class="flex flex-col items-center gap-2">
        <div class="flex h-9 w-9 items-center justify-center rounded-full bg-navy-950 text-sm font-bold text-white">✓</div>
        <span class="text-xs font-semibold text-navy-950">STEP 1<br />기본 자격 정보</span>
      </div>
      <div class="h-px w-16 bg-navy-950" />
      <div class="flex flex-col items-center gap-2">
        <div class="flex h-9 w-9 items-center justify-center rounded-full bg-navy-950 text-sm font-bold text-white">2</div>
        <span class="text-xs font-semibold text-navy-950">STEP 2<br />경력 및 선호 정보</span>
      </div>
    </div>

    <div class="rounded-xl border border-slate-200 p-6 sm:p-8">
      <h1 class="text-xl font-bold text-navy-950">상세 정보를 입력해주세요</h1>

      <label class="mt-6 block">
        <span class="text-sm font-medium text-navy-950">📁 상세 경력기술서</span>
        <textarea
          v-model="form.careerText"
          rows="6"
          minlength="100"
          maxlength="2000"
          placeholder="주요 담당 업무, 진행했던 프로젝트, 사용 기술 등을 구체적으로 작성해주세요. 구체적일수록 AI가 이민 및 취업 유사도를 더 정확히 분석할 수 있습니다."
          class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
          required
        />
        <div class="mt-1 text-right text-xs text-slate-400">{{ careerTextLength }} / 2000</div>
      </label>
      <p class="-mt-2 flex items-start gap-1 text-xs text-slate-400">
        <span aria-hidden="true">ⓘ</span>
        <span>구체적인 키워드(예: 클라우드 아키텍처, 회계 감사, 데이터 분석)를 포함하면 분석 정밀도가 향상됩니다.</span>
      </p>

      <div class="mt-6 grid gap-5 sm:grid-cols-2">
        <label class="block">
          <span class="text-sm font-medium text-navy-950">💳 보유 자금 구간</span>
          <select v-model="form.fundsRange" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" required>
            <option value="" disabled>구간 선택</option>
            <option value="UNDER_10M">1천만 원 미만</option>
            <option value="10M_30M">1천만~3천만 원</option>
            <option value="30M_50M">3천만~5천만 원</option>
            <option value="OVER_50M">5천만 원 이상</option>
          </select>
        </label>
        <div class="block">
          <span class="text-sm font-medium text-navy-950">👪 가족 동반 여부</span>
          <div class="mt-1 grid grid-cols-2 gap-2">
            <button
              type="button"
              class="rounded-lg border py-2.5 text-sm font-medium"
              :class="!form.familyAccompanied ? 'border-navy-950 bg-navy-950 text-white' : 'border-slate-300 text-slate-500'"
              @click="form.familyAccompanied = false"
            >
              단독 이주
            </button>
            <button
              type="button"
              class="rounded-lg border py-2.5 text-sm font-medium"
              :class="form.familyAccompanied ? 'border-navy-950 bg-navy-950 text-white' : 'border-slate-300 text-slate-500'"
              @click="form.familyAccompanied = true"
            >
              가족 동반
            </button>
          </div>
        </div>
      </div>

      <div class="mt-6">
        <span class="text-sm font-medium text-navy-950">🌍 관심 국가</span>
        <div class="mt-2 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <button
            v-for="country in interestCountries"
            :key="country.code"
            type="button"
            class="flex flex-col items-center gap-2 rounded-lg border py-4 text-sm font-medium"
            :class="form.preferredCountry === country.code ? 'border-navy-950 bg-soft-50 text-navy-950' : 'border-slate-300 text-slate-500'"
            @click="form.preferredCountry = country.code"
          >
            <span class="text-2xl">{{ country.icon }}</span>
            {{ country.label }}
          </button>
        </div>
      </div>

      <div class="mt-6 flex items-center justify-between border-t border-slate-100 pt-6">
        <button type="button" class="text-sm font-semibold text-navy-950" @click="goPrev">← 이전</button>
        <button
          type="button"
          class="rounded-lg bg-gold-500 px-6 py-3 text-sm font-semibold text-navy-950 hover:bg-gold-400 disabled:opacity-60"
          :disabled="submitting"
          @click="submit"
        >
          AI 분석 시작하기 ✨
        </button>
      </div>
      <p v-if="errorMessage" class="mt-3 text-right text-sm text-red-600">{{ errorMessage }}</p>
    </div>

    <div class="mt-6 flex items-start gap-2 rounded-lg border border-soft-100 bg-soft-50 px-4 py-3 text-xs text-slate-500">
      <span aria-hidden="true">🛡️</span>
      <span>
        <strong class="text-navy-950">개인정보 보호 및 데이터 활용 안내</strong><br />
        입력하신 정보는 AI 분석을 위해 사용됩니다. 분석 결과는 등록된 정책 정보와 공개 통계를 기반으로 제공되는
        참고용 적합도 분석이며, 법적 판단이나 비자 승인을 보장하지 않습니다.
      </span>
    </div>
  </section>
</template>
