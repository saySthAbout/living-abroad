<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import DisclaimerBox from '@/components/layout/DisclaimerBox.vue'

const router = useRouter()
const authStore = useAuthStore()

function startAnalysis() {
  router.push(authStore.token ? '/analysis/step-1' : '/auth?tab=login')
}
</script>

<template>
  <section class="mx-auto max-w-3xl space-y-8 text-center">
    <h1 class="text-3xl font-bold">Living Abroad</h1>
    <p class="text-gray-600">
      캐나다·호주·영국 기술이민을 준비하는 당신을 위한 AI 기반 국가·비자 적합도 분석
    </p>
    <button
      class="rounded bg-gray-900 px-6 py-3 text-white"
      @click="startAnalysis"
    >
      AI 분석 시작
    </button>
    <div class="grid grid-cols-1 gap-4 text-left sm:grid-cols-3">
      <div class="rounded border border-gray-200 p-4">
        <h2 class="font-semibold">캐나다</h2>
        <p class="text-sm text-gray-500">Express Entry FSW</p>
      </div>
      <div class="rounded border border-gray-200 p-4">
        <h2 class="font-semibold">호주</h2>
        <p class="text-sm text-gray-500">Skilled Independent Visa 189</p>
      </div>
      <div class="rounded border border-gray-200 p-4">
        <h2 class="font-semibold">영국</h2>
        <p class="text-sm text-gray-500">Skilled Worker Visa</p>
      </div>
    </div>
    <DisclaimerBox />
  </section>
</template>
