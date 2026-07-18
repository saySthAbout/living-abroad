<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { apiClient } from '@/api/client'
import LoadingSpinner from '@/components/layout/LoadingSpinner.vue'

interface AnalysisHistoryItem {
  analysisId: number
  topCountryCode: string
  topVisaName: string
  topScore: number
  analyzedAt: string
}

const items = ref<AnalysisHistoryItem[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const { data } = await apiClient.get('/api/analyses')
    items.value = data.items ?? []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="mx-auto max-w-2xl space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-semibold">내 분석 결과</h1>
      <RouterLink to="/analysis/step-1" class="rounded bg-gray-900 px-4 py-2 text-sm text-white">새 분석 시작</RouterLink>
    </div>

    <LoadingSpinner v-if="loading" />
    <p v-else-if="items.length === 0" class="text-sm text-gray-500">저장된 분석 결과가 없습니다.</p>
    <RouterLink
      v-for="item in items"
      :key="item.analysisId"
      :to="`/results/${item.analysisId}`"
      class="block rounded border border-gray-200 p-4"
    >
      <p class="text-sm text-gray-400">{{ item.analyzedAt }}</p>
      <p class="font-semibold">{{ item.topCountryCode }} · {{ item.topVisaName }} · {{ item.topScore }}점</p>
    </RouterLink>
  </section>
</template>
