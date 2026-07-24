<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAnalysisStore } from '@/stores/analysis'
import { getErrorMessage } from '@/api/client'

const router = useRouter()
const analysisStore = useAnalysisStore()
const { t } = useI18n()

const form = reactive({ ...analysisStore.step2Data })
const errorMessage = ref('')
const submitting = ref(false)

const careerTextLength = computed(() => form.careerText.length)

const interestCountries = computed(() => [
  { code: 'CAN', label: t('step2.countryCan'), icon: '🇨🇦' },
  { code: 'AUS', label: t('step2.countryAus'), icon: '🇦🇺' },
  { code: 'GBR', label: t('step2.countryGbr'), icon: '🇬🇧' },
  { code: 'ANY', label: t('step2.countryAny'), icon: '🌐' },
])

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
  } catch (error) {
    errorMessage.value = getErrorMessage(error, t('step2.errorFallback'))
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
        <span class="text-xs font-semibold text-navy-950">STEP 1<br />{{ t('step1.stepLabel1') }}</span>
      </div>
      <div class="h-px w-16 bg-navy-950" />
      <div class="flex flex-col items-center gap-2">
        <div class="flex h-9 w-9 items-center justify-center rounded-full bg-navy-950 text-sm font-bold text-white">2</div>
        <span class="text-xs font-semibold text-navy-950">STEP 2<br />{{ t('step1.stepLabel2') }}</span>
      </div>
    </div>

    <div class="rounded-xl border border-slate-200 p-6 sm:p-8">
      <h1 class="text-xl font-bold text-navy-950">{{ t('step2.title') }}</h1>

      <label class="mt-6 block">
        <span class="text-sm font-medium text-navy-950">{{ t('step2.careerText') }}</span>
        <textarea
          v-model="form.careerText"
          rows="6"
          minlength="100"
          maxlength="2000"
          :placeholder="t('step2.careerTextPlaceholder')"
          class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
          required
        />
        <div class="mt-1 text-right text-xs text-slate-400">{{ careerTextLength }} / 2000</div>
      </label>
      <p class="-mt-2 flex items-start gap-1 text-xs text-slate-400">
        <span aria-hidden="true">ⓘ</span>
        <span>{{ t('step2.careerTextHint') }}</span>
      </p>

      <div class="mt-6 grid gap-5 sm:grid-cols-2">
        <label class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('step2.fundsRange') }}</span>
          <select v-model="form.fundsRange" class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none" required>
            <option value="" disabled>{{ t('step2.fundsRangeSelect') }}</option>
            <option value="UNDER_10M">{{ t('step2.fundsUnder10M') }}</option>
            <option value="10M_30M">{{ t('step2.funds10to30M') }}</option>
            <option value="30M_50M">{{ t('step2.funds30to50M') }}</option>
            <option value="OVER_50M">{{ t('step2.fundsOver50M') }}</option>
          </select>
        </label>
        <div class="block">
          <span class="text-sm font-medium text-navy-950">{{ t('step2.familyAccompanied') }}</span>
          <div class="mt-1 grid grid-cols-2 gap-2">
            <button
              type="button"
              class="rounded-lg border py-2.5 text-sm font-medium"
              :class="!form.familyAccompanied ? 'border-navy-950 bg-navy-950 text-white' : 'border-slate-300 text-slate-500'"
              @click="form.familyAccompanied = false"
            >
              {{ t('step2.alone') }}
            </button>
            <button
              type="button"
              class="rounded-lg border py-2.5 text-sm font-medium"
              :class="form.familyAccompanied ? 'border-navy-950 bg-navy-950 text-white' : 'border-slate-300 text-slate-500'"
              @click="form.familyAccompanied = true"
            >
              {{ t('step2.withFamily') }}
            </button>
          </div>
        </div>
      </div>

      <div class="mt-6">
        <span class="text-sm font-medium text-navy-950">{{ t('step2.preferredCountry') }}</span>
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
        <button type="button" class="text-sm font-semibold text-navy-950" @click="goPrev">{{ t('step2.prev') }}</button>
        <button
          type="button"
          class="rounded-lg bg-gold-500 px-6 py-3 text-sm font-semibold text-navy-950 hover:bg-gold-400 disabled:opacity-60"
          :disabled="submitting"
          @click="submit"
        >
          {{ t('step2.submit') }}
        </button>
      </div>
      <p v-if="errorMessage" class="mt-3 text-right text-sm text-red-600">{{ errorMessage }}</p>
    </div>

    <div class="mt-6 flex items-start gap-2 rounded-lg border border-soft-100 bg-soft-50 px-4 py-3 text-xs text-slate-500">
      <span aria-hidden="true">🛡️</span>
      <span>
        <strong class="text-navy-950">{{ t('step2.privacyTitle') }}</strong><br />
        {{ t('step2.privacyBody') }}
      </span>
    </div>
  </section>
</template>
