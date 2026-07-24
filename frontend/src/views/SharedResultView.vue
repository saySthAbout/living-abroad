<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()
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
    loadError.value = t('sharedResult.invalidLink')
    loading.value = false
    return
  }
  try {
    result.value = (await analysisStore.loadSharedResult(token)) as unknown as AnalysisDetail
  } catch (error) {
    loadError.value = getErrorMessage(error, t('sharedResult.loadErrorFallback'))
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="mx-auto max-w-6xl px-6 py-10">
    <div class="rounded-lg border border-navy-100 bg-soft-50 px-4 py-3 text-sm text-navy-700">
      {{ t('sharedResult.banner') }}
    </div>

    <h1 class="mt-6 flex items-center gap-2 text-2xl font-bold text-navy-950">{{ t('result.title') }}</h1>

    <LoadingSpinner v-if="loading" />
    <p v-else-if="loadError" class="mt-8 text-sm text-red-600">{{ loadError }}</p>

    <template v-else-if="result">
      <h2 class="mt-8 text-lg font-bold text-navy-950">{{ t('result.topCountries') }}</h2>
      <div class="mt-4 grid gap-6 lg:grid-cols-3">
        <div
          v-for="item in sortedResults"
          :key="item.countryCode"
          class="rounded-xl border-2 p-5"
          :class="item.rank === 1 ? 'border-gold-500' : 'border-slate-200'"
        >
          <span v-if="item.rank === 1" class="mb-3 inline-block rounded bg-gold-500 px-2 py-1 text-xs font-bold text-navy-950">
            {{ t('result.bestMatch') }}
          </span>
          <h3 class="text-lg font-bold text-navy-950">{{ flagFor(item.countryCode) }} {{ item.countryName }}</h3>

          <div class="mt-3 flex items-end justify-between">
            <div class="text-xs text-slate-400">
              {{ t('result.representativeVisa') }}<br />
              <span class="text-sm font-medium text-navy-950">{{ item.visaName }}</span>
            </div>
            <div class="text-right">
              <span class="text-3xl font-extrabold text-navy-950">{{ item.totalScore }}</span>
              <p class="text-xs text-slate-400">{{ t('result.totalScore') }}</p>
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
        <DisclaimerBox :text="result.disclaimer ?? t('result.disclaimerDefault')" />
        <RouterLink to="/" class="shrink-0 rounded-lg bg-navy-950 px-4 py-2.5 text-sm font-semibold text-white hover:bg-navy-900">
          {{ t('sharedResult.tryItYourself') }}
        </RouterLink>
      </div>
    </template>
  </section>
</template>
