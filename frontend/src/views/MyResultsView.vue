<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
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
const { t, locale } = useI18n()
const items = ref<AnalysisHistoryItem[]>([])
const loading = ref(true)
const loadError = ref('')

const latestAnalyzedAt = computed(() => items.value[0]?.analyzedAt?.slice(0, 10))
const disclaimerText = computed(
  () => t('myResults.disclaimerBase') + (latestAnalyzedAt.value ? t('myResults.disclaimerDateSuffix', { date: latestAnalyzedAt.value }) : ''),
)

function flagFor(code: string) {
  return COUNTRIES.find((country) => country.code === code)?.flag ?? '🌐'
}

function countryNameFor(code: string) {
  const country = COUNTRIES.find((c) => c.code === code)
  if (!country) return code
  return locale.value === 'ko' ? country.nameKo : country.nameEn
}

onMounted(async () => {
  try {
    const { data } = await apiClient.get('/api/analyses')
    items.value = data.items ?? []
  } catch (error) {
    loadError.value = getErrorMessage(error, t('myResults.loadError'))
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
          <h1 class="text-2xl font-bold">{{ t('myResults.greeting', { name: authStore.user?.name ?? t('myResults.member') }) }}</h1>
          <p v-if="latestAnalyzedAt" class="mt-3 text-sm text-white/70">{{ t('myResults.lastAnalyzedAt', { date: latestAnalyzedAt }) }}</p>
        </div>
        <RouterLink to="/analysis/step-1?mode=edit" class="rounded-lg bg-white/10 px-4 py-2 text-sm font-semibold hover:bg-white/20">
          {{ t('myResults.editProfile') }}
        </RouterLink>
      </div>
    </div>

    <div class="mt-8 flex items-center justify-between">
      <h2 class="flex items-center gap-2 text-lg font-bold text-navy-950">{{ t('myResults.recentHistory') }}</h2>
      <span class="text-sm text-slate-400">{{ t('myResults.totalCount', { count: items.length }) }}</span>
    </div>

    <LoadingSpinner v-if="loading" />
    <p v-else-if="loadError" class="mt-4 text-sm text-red-600">{{ loadError }}</p>
    <p v-else-if="items.length === 0" class="mt-4 rounded-xl border border-slate-200 px-6 py-10 text-center text-sm text-slate-500">
      {{ t('myResults.empty') }}
      <RouterLink to="/analysis/step-1" class="ml-1 font-semibold text-navy-950">{{ t('myResults.startFirst') }}</RouterLink>
    </p>

    <div v-else class="mt-4 space-y-4">
      <div v-for="item in items" :key="item.analysisId" class="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-200 p-5">
        <div>
          <p class="text-xs text-slate-400">{{ t('myResults.analyzedAt', { date: item.analyzedAt?.slice(0, 10) }) }}</p>
          <p class="mt-1 font-semibold text-navy-950">
            {{ flagFor(item.topCountryCode) }} {{ t('myResults.topRecommendation', { country: countryNameFor(item.topCountryCode), code: item.topCountryCode }) }}
          </p>
          <p class="text-sm text-slate-500">{{ t('myResults.representativeVisa', { visa: item.topVisaName }) }}</p>
        </div>
        <div class="flex items-center gap-6">
          <div class="text-right">
            <p class="text-2xl font-extrabold text-navy-950">
              {{ item.topScore }}<span class="text-sm font-medium text-slate-400"> {{ t('myResults.scoreUnit') }}</span>
            </p>
            <p class="text-[11px] tracking-wide text-slate-400">{{ t('myResults.suitabilityScore') }}</p>
          </div>
          <RouterLink :to="`/results/${item.analysisId}`" class="rounded-lg bg-navy-950 px-4 py-2.5 text-sm font-semibold text-white hover:bg-navy-900">
            {{ t('myResults.viewAgain') }}
          </RouterLink>
        </div>
      </div>
    </div>

    <div class="mt-8">
      <DisclaimerBox :text="disclaimerText" />
    </div>
  </section>
</template>
