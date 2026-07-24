<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()
const loading = ref(true)
const loadError = ref('')

const checklistItems = computed(() => [
  t('result.checklistItem1'),
  t('result.checklistItem2'),
  t('result.checklistItem3'),
  t('result.checklistItem4'),
  t('result.checklistItem5'),
])
const checkedItems = ref<boolean[]>(checklistItems.value.map(() => false))

const result = computed(() => analysisStore.analysisResult as unknown as AnalysisDetail | null)
const sortedResults = computed(() => [...(result.value?.results ?? [])].sort((a, b) => a.rank - b.rank))

const shareStatus = ref<'idle' | 'creating' | 'ready' | 'error'>('idle')
const shareUrl = ref('')
const shareErrorMessage = ref('')
const copyLabel = ref(t('result.copyLink'))

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
    shareErrorMessage.value = getErrorMessage(error, t('result.shareErrorFallback'))
  }
}

async function copyShareUrl() {
  await navigator.clipboard.writeText(shareUrl.value)
  copyLabel.value = t('result.copied')
  setTimeout(() => {
    copyLabel.value = t('result.copyLink')
  }, 2000)
}

onMounted(async () => {
  const analysisId = Number(route.params.id)
  try {
    await analysisStore.loadResult(analysisId)
  } catch (error) {
    loadError.value = getErrorMessage(error, t('result.loadError'))
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="mx-auto max-w-6xl px-6 py-10">
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <h1 class="flex items-center gap-2 text-2xl font-bold text-navy-950">{{ t('result.title') }}</h1>
        <p class="mt-2 text-sm text-slate-500">
          {{ authStore.user?.name ?? t('result.member') }}
          <span v-if="result">· {{ t('result.analyzedAt', { date: result.analyzedAt?.slice(0, 10) }) }}</span>
        </p>
      </div>
      <span class="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-500">
        {{ t('result.scoreInfo') }}
      </span>
    </div>

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

          <div class="mt-4 space-y-2 text-xs">
            <div>
              <div class="flex justify-between text-slate-500"><span>{{ t('result.environmentScore') }}</span><span>{{ item.environmentScore }}/100</span></div>
              <div class="mt-1 h-1.5 rounded-full bg-soft-100">
                <div class="h-full rounded-full bg-navy-700" :style="{ width: `${item.environmentScore}%` }" />
              </div>
            </div>
            <div>
              <div class="flex justify-between text-slate-500"><span>{{ t('result.careerSimilarity') }}</span><span>{{ item.careerSimilarity }}/100</span></div>
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

      <h2 class="mt-10 flex items-center gap-2 text-lg font-bold text-navy-950">{{ t('result.comparison') }}</h2>
      <div class="mt-4 overflow-x-auto rounded-xl border border-slate-200">
        <table class="w-full text-left text-sm">
          <thead class="bg-soft-50 text-slate-500">
            <tr>
              <th class="px-4 py-3 font-medium">{{ t('result.metric') }}</th>
              <th v-for="item in sortedResults" :key="item.countryCode" class="px-4 py-3 font-medium">{{ item.countryName }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr>
              <td class="px-4 py-3 font-medium text-navy-950">{{ t('result.totalScore') }}</td>
              <td v-for="item in sortedResults" :key="item.countryCode" class="px-4 py-3 font-bold text-navy-950">{{ item.totalScore }}{{ t('result.points') }}</td>
            </tr>
            <tr>
              <td class="px-4 py-3 font-medium text-navy-950">{{ t('result.environmentScore') }}</td>
              <td v-for="item in sortedResults" :key="item.countryCode" class="px-4 py-3 text-slate-600">{{ item.environmentScore }}{{ t('result.points') }}</td>
            </tr>
            <tr>
              <td class="px-4 py-3 font-medium text-navy-950">{{ t('result.careerSimilarity') }}</td>
              <td v-for="item in sortedResults" :key="item.countryCode" class="px-4 py-3 text-slate-600">{{ item.careerSimilarity }}{{ t('result.points') }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <h2 class="mt-10 flex items-center gap-2 text-lg font-bold text-navy-950">{{ t('result.checklist') }}</h2>
      <div class="mt-4 grid gap-3 sm:grid-cols-2">
        <label v-for="(item, index) in checklistItems" :key="item" class="flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm">
          <input v-model="checkedItems[index]" type="checkbox" class="h-4 w-4 accent-navy-950" />
          {{ item }}
        </label>
      </div>

      <div class="mt-8 flex flex-wrap items-center justify-between gap-4 border-t border-slate-100 pt-6">
        <DisclaimerBox :text="result.disclaimer ?? t('result.disclaimerDefault')" />
        <div class="flex shrink-0 flex-wrap gap-3 print:hidden">
          <button class="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-600" @click="exportPdf">
            {{ t('result.exportPdf') }}
          </button>
          <button class="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-600" @click="createShare">
            {{ t('result.share') }}
          </button>
          <button class="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-600" @click="router.push('/analysis/step-1')">
            {{ t('result.reanalyze') }}
          </button>
          <button class="rounded-lg bg-navy-950 px-4 py-2.5 text-sm font-semibold text-white hover:bg-navy-900" @click="router.push('/chat')">
            {{ t('result.askAi') }}
          </button>
        </div>
      </div>

      <div v-if="shareStatus !== 'idle'" class="mt-4 rounded-lg border border-slate-200 bg-soft-50 p-4 text-sm print:hidden">
        <template v-if="shareStatus === 'creating'">{{ t('result.shareCreating') }}</template>
        <template v-else-if="shareStatus === 'ready'">
          <p class="text-slate-500">{{ t('result.shareDesc') }}</p>
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
