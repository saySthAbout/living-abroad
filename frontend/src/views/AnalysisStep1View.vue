<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAnalysisStore } from '@/stores/analysis'
import DisclaimerBox from '@/components/layout/DisclaimerBox.vue'
import { COUNTRIES } from '@/constants/countries'

const router = useRouter()
const analysisStore = useAnalysisStore()

const form = reactive({ ...analysisStore.step1Data })

function goNext() {
  analysisStore.saveStep1({ ...form })
  router.push('/analysis/step-2')
}
</script>

<template>
  <section class="mx-auto max-w-3xl px-6 py-12">
    <div class="mb-10 flex items-center justify-center gap-4">
      <div class="flex flex-col items-center gap-2">
        <div class="flex h-9 w-9 items-center justify-center rounded-full bg-navy-950 text-sm font-bold text-white">1</div>
        <span class="text-xs font-semibold text-navy-950">STEP 1<br />기본 자격 정보</span>
      </div>
      <div class="h-px w-16 bg-slate-200" />
      <div class="flex flex-col items-center gap-2">
        <div class="flex h-9 w-9 items-center justify-center rounded-full bg-soft-100 text-sm font-bold text-slate-400">2</div>
        <span class="text-xs font-semibold text-slate-400">STEP 2<br />경력 및 선호 정보</span>
      </div>
    </div>

    <h1 class="text-center text-2xl font-bold text-navy-950">정확한 분석을 위해<br />기본 정보를 입력해주세요.</h1>
    <p class="mx-auto mt-3 max-w-xl text-center text-sm text-slate-500">
      입력하신 정보는 캐나다·호주·영국의 공개된 자격 요건과 통계 데이터를 기반으로 국가·비자 적합도를
      분석하는 데 활용됩니다.
    </p>

    <div class="mt-8 rounded-xl border border-slate-200 p-6 sm:p-8">
      <div class="grid gap-5 sm:grid-cols-2">
        <label class="block">
          <span class="text-sm font-medium text-navy-950">나이 (만)</span>
          <input v-model.number="form.age" type="number" placeholder="예: 30" min="18" max="64" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" required />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">최종 학력</span>
          <select v-model="form.education" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" required>
            <option value="" disabled>학력 선택</option>
            <option value="HIGH_SCHOOL">고등학교 졸업</option>
            <option value="ASSOCIATE">전문학사</option>
            <option value="BACHELOR">학사</option>
            <option value="MASTER">석사</option>
            <option value="DOCTOR">박사</option>
          </select>
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">전공</span>
          <input v-model="form.major" type="text" placeholder="예: 컴퓨터공학" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" required />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">현재 직업</span>
          <input v-model="form.occupation" type="text" placeholder="예: 소프트웨어 엔지니어" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" required />
        </label>
      </div>

      <label class="mt-5 block">
        <span class="text-sm font-medium text-navy-950">총 관련 경력 연수</span>
        <div class="mt-1 flex items-center gap-2">
          <input v-model.number="form.experienceYears" type="number" placeholder="예: 5" min="0" max="40" class="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" required />
          <span class="text-sm text-slate-500">년</span>
        </div>
      </label>

      <div class="mt-5 grid gap-5 border-t border-slate-100 pt-5 sm:grid-cols-2">
        <label class="block">
          <span class="text-sm font-medium text-navy-950">영어시험 종류</span>
          <select v-model="form.languageTest" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none">
            <option :value="null">미응시</option>
            <option value="IELTS_GENERAL">IELTS Academic/General</option>
            <option value="CELPIP">CELPIP</option>
            <option value="PTE">PTE</option>
          </select>
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">영어 종합점수 (Overall)</span>
          <input v-model.number="form.languageScore" type="number" step="0.1" placeholder="예: 7.0" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" />
        </label>
      </div>

      <div class="mt-6">
        <DisclaimerBox text="모든 분석 결과는 공신력 있는 내부 분석 알고리즘에 기초하지만, 공식적인 법적 자문을 대체할 수 없습니다. 정보 기준일: 2026.07.21" />
      </div>

      <button class="mt-6 w-full rounded-lg bg-gold-500 py-3 text-sm font-semibold text-navy-950 hover:bg-gold-400" @click="goNext">
        다음 단계로 →
      </button>
    </div>

    <div class="mt-8 grid gap-4 sm:grid-cols-3">
      <div v-for="country in COUNTRIES" :key="country.code" :class="['flex h-24 items-end rounded-lg bg-gradient-to-br p-3', country.gradient]">
        <span class="text-xs font-semibold text-white">{{ country.flag }} {{ country.nameEn }}</span>
      </div>
    </div>
  </section>
</template>
