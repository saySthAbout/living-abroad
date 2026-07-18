<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAnalysisStore } from '@/stores/analysis'

const router = useRouter()
const analysisStore = useAnalysisStore()

const form = reactive({ ...analysisStore.step2Data })
const errorMessage = ref('')

function goPrev() {
  router.push('/analysis/step-1')
}

async function submit() {
  errorMessage.value = ''
  analysisStore.saveStep2({ ...form })
  try {
    await analysisStore.submitAnalysis()
    router.push('/analysis/loading')
  } catch {
    errorMessage.value = '분석 요청 중 오류가 발생했습니다.'
  }
}
</script>

<template>
  <section class="mx-auto max-w-lg space-y-6">
    <p class="text-sm text-gray-400">2 / 2 단계</p>
    <h1 class="text-xl font-semibold">상세 정보 입력</h1>

    <div class="space-y-4">
      <label class="block">
        <span class="text-sm">언어시험</span>
        <select v-model="form.languageTest" class="mt-1 w-full rounded border border-gray-300 px-3 py-2">
          <option :value="null">미응시</option>
          <option value="IELTS_GENERAL">IELTS General</option>
          <option value="CELPIP">CELPIP</option>
          <option value="PTE">PTE</option>
        </select>
      </label>
      <label class="block">
        <span class="text-sm">언어 점수</span>
        <input v-model.number="form.languageScore" type="number" step="0.1" class="mt-1 w-full rounded border border-gray-300 px-3 py-2" />
      </label>
      <label class="block">
        <span class="text-sm">보유 자금</span>
        <select v-model="form.fundsRange" class="mt-1 w-full rounded border border-gray-300 px-3 py-2" required>
          <option value="" disabled>선택</option>
          <option value="UNDER_10M">1천만 원 미만</option>
          <option value="10M_30M">1천만~3천만 원</option>
          <option value="30M_50M">3천만~5천만 원</option>
          <option value="OVER_50M">5천만 원 이상</option>
        </select>
      </label>
      <label class="flex items-center gap-2">
        <input v-model="form.familyAccompanied" type="checkbox" />
        <span class="text-sm">가족 동반</span>
      </label>
      <label class="block">
        <span class="text-sm">선호 국가</span>
        <select v-model="form.preferredCountry" class="mt-1 w-full rounded border border-gray-300 px-3 py-2" required>
          <option value="" disabled>선택</option>
          <option value="CAN">캐나다</option>
          <option value="AUS">호주</option>
          <option value="GBR">영국</option>
          <option value="ANY">상관없음</option>
        </select>
      </label>
      <label class="block">
        <span class="text-sm">경력기술서 (100~2000자)</span>
        <textarea
          v-model="form.careerText"
          rows="6"
          minlength="100"
          maxlength="2000"
          class="mt-1 w-full rounded border border-gray-300 px-3 py-2"
          required
        />
      </label>
    </div>

    <div class="flex gap-3">
      <button class="flex-1 rounded border border-gray-300 py-2" @click="goPrev">이전</button>
      <button class="flex-1 rounded bg-gray-900 py-2 text-white" @click="submit">AI 분석 시작</button>
    </div>
    <p v-if="errorMessage" class="text-sm text-red-600">{{ errorMessage }}</p>
  </section>
</template>
