<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAnalysisStore } from '@/stores/analysis'
import DisclaimerBox from '@/components/layout/DisclaimerBox.vue'
import { COUNTRIES } from '@/constants/countries'
import { apiClient } from '@/api/client'

const route = useRoute()
const router = useRouter()
const analysisStore = useAnalysisStore()
const { t } = useI18n()

const form = reactive({ ...analysisStore.step1Data })
const formEl = ref<HTMLFormElement | null>(null)
const errorMessage = ref('')

onMounted(async () => {
  if (route.query.mode !== 'edit') return
  try {
    const { data } = await apiClient.get('/api/analyses/latest-input')
    Object.assign(form, {
      age: data.age,
      education: data.education,
      major: data.major,
      occupation: data.occupation,
      experienceYears: data.experienceYears,
      languageTest: data.languageTest,
      languageScore: data.languageScore,
    })
    analysisStore.saveStep2({
      careerText: data.careerText,
      fundsRange: data.fundsRange,
      familyAccompanied: data.familyAccompanied,
      preferredCountry: data.preferredCountry,
    })
  } catch {
    // 이전 분석 이력이 없으면(예: 첫 분석 전) 빈 폼으로 그냥 진행한다.
  }
})

function blockNonNumericKey(event: KeyboardEvent) {
  if (['e', 'E', '+', '-'].includes(event.key)) event.preventDefault()
}

function goNext() {
  const invalidField = formEl.value?.querySelector<HTMLInputElement | HTMLSelectElement>(':invalid')
  if (invalidField) {
    errorMessage.value = invalidField.validationMessage || t('step1.validationError')
    return
  }
  errorMessage.value = ''
  analysisStore.saveStep1({ ...form })
  router.push('/analysis/step-2')
}
</script>

<template>
  <section class="mx-auto max-w-3xl px-6 py-12">
    <div class="mb-10 flex items-center justify-center gap-4">
      <div class="flex flex-col items-center gap-2">
        <div class="flex h-9 w-9 items-center justify-center rounded-full bg-navy-950 text-sm font-bold text-white">1</div>
        <span class="text-xs font-semibold text-navy-950">STEP 1<br />{{ t('step1.stepLabel1') }}</span>
      </div>
      <div class="h-px w-16 bg-slate-200" />
      <div class="flex flex-col items-center gap-2">
        <div class="flex h-9 w-9 items-center justify-center rounded-full bg-soft-100 text-sm font-bold text-slate-400">2</div>
        <span class="text-xs font-semibold text-slate-400">STEP 2<br />{{ t('step1.stepLabel2') }}</span>
      </div>
    </div>

    <h1 class="text-center text-2xl font-bold text-navy-950">{{ t('step1.titleLine1') }}<br />{{ t('step1.titleLine2') }}</h1>
    <p class="mx-auto mt-3 max-w-xl text-center text-sm text-slate-500">
      {{ t('step1.subtitle') }}
    </p>

    <form ref="formEl" class="mt-8 rounded-xl border border-slate-200 p-6 sm:p-8" @submit.prevent="goNext">
      <div class="grid gap-5 sm:grid-cols-2">
        <label class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('step1.age') }}</span>
          <input v-model.number="form.age" type="number" :placeholder="t('step1.agePlaceholder')" min="18" max="64" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" required />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('step1.education') }}</span>
          <select v-model="form.education" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" required>
            <option value="" disabled>{{ t('step1.educationSelect') }}</option>
            <option value="HIGH_SCHOOL">{{ t('step1.educationHighSchool') }}</option>
            <option value="ASSOCIATE">{{ t('step1.educationAssociate') }}</option>
            <option value="BACHELOR">{{ t('step1.educationBachelor') }}</option>
            <option value="MASTER">{{ t('step1.educationMaster') }}</option>
            <option value="DOCTOR">{{ t('step1.educationDoctor') }}</option>
          </select>
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('step1.major') }}</span>
          <input v-model="form.major" type="text" :placeholder="t('step1.majorPlaceholder')" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" required />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('step1.occupation') }}</span>
          <input v-model="form.occupation" type="text" :placeholder="t('step1.occupationPlaceholder')" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" required />
        </label>
      </div>

      <label class="mt-5 block">
        <span class="text-sm font-medium text-navy-950">{{ t('step1.experienceYears') }}</span>
        <div class="mt-1 flex items-center gap-2">
          <input v-model.number="form.experienceYears" type="number" :placeholder="t('step1.experienceYearsPlaceholder')" min="0" max="40" class="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" required />
          <span class="text-sm text-slate-500">{{ t('step1.years') }}</span>
        </div>
      </label>

      <div class="mt-5 grid gap-5 border-t border-slate-100 pt-5 sm:grid-cols-2">
        <label class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('step1.languageTest') }}</span>
          <select v-model="form.languageTest" @change="form.languageTest === null && (form.languageScore = null)" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none">
            <option :value="null">{{ t('step1.languageTestNone') }}</option>
            <option value="IELTS_GENERAL">IELTS Academic/General</option>
            <option value="CELPIP">CELPIP</option>
            <option value="PTE">PTE</option>
          </select>
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('step1.languageScore') }}</span>
          <input v-model.number="form.languageScore" type="number" step="0.1" min="0" inputmode="decimal" @keydown="blockNonNumericKey" :placeholder="t('step1.languageScorePlaceholder')" :disabled="!form.languageTest" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none disabled:cursor-not-allowed disabled:bg-soft-100 disabled:text-slate-400" />
        </label>
      </div>

      <div class="mt-6">
        <DisclaimerBox :text="t('step1.disclaimer')" />
      </div>

      <button type="submit" class="mt-6 w-full rounded-lg bg-gold-500 py-3 text-sm font-semibold text-navy-950 hover:bg-gold-400">
        {{ t('step1.nextButton') }}
      </button>
      <p v-if="errorMessage" class="mt-3 text-center text-sm text-red-600">{{ errorMessage }}</p>
    </form>

    <div class="mt-8 grid gap-4 sm:grid-cols-3">
      <div v-for="country in COUNTRIES" :key="country.code" :class="['flex h-24 items-end rounded-lg bg-gradient-to-br p-3', country.gradient]">
        <span class="text-xs font-semibold text-white">{{ country.flag }} {{ country.nameEn }}</span>
      </div>
    </div>
  </section>
</template>
