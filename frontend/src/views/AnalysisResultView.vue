<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAnalysisStore } from '@/stores/analysis'
import DisclaimerBox from '@/components/layout/DisclaimerBox.vue'
import LoadingSpinner from '@/components/layout/LoadingSpinner.vue'

const route = useRoute()
const analysisStore = useAnalysisStore()
const loading = ref(true)

onMounted(async () => {
  const analysisId = Number(route.params.id)
  await analysisStore.loadResult(analysisId)
  loading.value = false
})
</script>

<template>
  <section class="mx-auto max-w-3xl space-y-6">
    <h1 class="text-xl font-semibold">AI 분석 결과</h1>
    <LoadingSpinner v-if="loading" />
    <template v-else-if="analysisStore.analysisResult">
      <div
        v-for="(result, index) in (analysisStore.analysisResult.results as Array<Record<string, unknown>>) ?? []"
        :key="index"
        class="rounded border border-gray-200 p-4"
      >
        <p class="text-sm text-gray-400">{{ (result.rank as number) }}순위</p>
        <h2 class="font-semibold">{{ result.countryName }} · {{ result.visaName }}</h2>
        <p class="text-2xl font-bold">{{ result.totalScore }}점</p>
      </div>
    </template>
    <DisclaimerBox />
  </section>
</template>
