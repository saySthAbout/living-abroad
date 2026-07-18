<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAnalysisStore } from '@/stores/analysis'

const router = useRouter()
const analysisStore = useAnalysisStore()

const form = reactive({ ...analysisStore.step1Data })

function goNext() {
  analysisStore.saveStep1({ ...form })
  router.push('/analysis/step-2')
}
</script>

<template>
  <section class="mx-auto max-w-lg space-y-6">
    <p class="text-sm text-gray-400">1 / 2 단계</p>
    <h1 class="text-xl font-semibold">기본 정보 입력</h1>

    <div class="space-y-4">
      <label class="block">
        <span class="text-sm">나이</span>
        <input v-model.number="form.age" type="number" min="18" max="64" class="mt-1 w-full rounded border border-gray-300 px-3 py-2" required />
      </label>
      <label class="block">
        <span class="text-sm">최종 학력</span>
        <select v-model="form.education" class="mt-1 w-full rounded border border-gray-300 px-3 py-2" required>
          <option value="" disabled>선택</option>
          <option value="HIGH_SCHOOL">고등학교 졸업</option>
          <option value="ASSOCIATE">전문학사</option>
          <option value="BACHELOR">학사</option>
          <option value="MASTER">석사</option>
          <option value="DOCTOR">박사</option>
        </select>
      </label>
      <label class="block">
        <span class="text-sm">전공</span>
        <input v-model="form.major" type="text" class="mt-1 w-full rounded border border-gray-300 px-3 py-2" required />
      </label>
      <label class="block">
        <span class="text-sm">현재 직업</span>
        <input v-model="form.occupation" type="text" class="mt-1 w-full rounded border border-gray-300 px-3 py-2" required />
      </label>
      <label class="block">
        <span class="text-sm">경력 연수</span>
        <input v-model.number="form.experienceYears" type="number" min="0" max="40" class="mt-1 w-full rounded border border-gray-300 px-3 py-2" required />
      </label>
    </div>

    <button class="w-full rounded bg-gray-900 py-2 text-white" @click="goNext">다음</button>
  </section>
</template>
