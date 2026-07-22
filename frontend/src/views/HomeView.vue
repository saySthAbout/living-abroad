<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import DisclaimerBox from '@/components/layout/DisclaimerBox.vue'
import { COUNTRIES } from '@/constants/countries'

const router = useRouter()
const authStore = useAuthStore()

function startAnalysis() {
  router.push(authStore.token ? '/analysis/step-1' : '/auth?tab=login')
}

function startChat() {
  router.push(authStore.token ? '/chat' : '/auth?tab=login')
}

const features = [
  {
    icon: '🛡️',
    title: '국가·비자 적합도 분석',
    description: '공개된 자격 요건과 통계 데이터를 기반으로 국가·비자 적합도를 분석합니다.',
  },
  {
    icon: '💼',
    title: '경력·해외 직업군 AI 매칭',
    description: '한국에서의 경력을 해외 현지 수요가 높은 직업군과 매칭하여 가장 효율적인 취업 전략을 세워드립니다.',
  },
  {
    icon: '💬',
    title: '공식 정책 기반 AI 상담',
    description: '등록된 공식 정책 문서를 검색하여 출처와 정보 기준일을 포함한 답변을 제공합니다.',
  },
]
</script>

<template>
  <div>
    <section class="bg-soft-50 px-6 py-20">
      <div class="mx-auto max-w-3xl text-center">
        <h1 class="text-4xl font-extrabold leading-tight text-navy-950 sm:text-5xl">
          AI로 찾는 나에게 맞는<br />해외 취업·이주 경로
        </h1>
        <p class="mt-6 text-slate-500">
          복잡한 이민 정책과 비자 조건을 일일이 확인하지 마세요. Living Abroad의 AI가 당신의 경력과 학력을
          분석하여 캐나다, 호주, 영국 중 가장 적합한 경로를 제안합니다.
        </p>
        <div class="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
          <button
            class="rounded-lg bg-gold-500 px-6 py-3 font-semibold text-navy-950 hover:bg-gold-400"
            @click="startAnalysis"
          >
            무료 AI 분석 시작하기
          </button>
          <button
            class="rounded-lg border border-navy-950 px-6 py-3 font-semibold text-navy-950 hover:bg-white"
            @click="startChat"
          >
            AI 상담 시작하기
          </button>
        </div>
      </div>
    </section>

    <section class="mx-auto max-w-6xl px-6 py-16">
      <div class="grid gap-6 sm:grid-cols-3">
        <div v-for="feature in features" :key="feature.title" class="rounded-xl border border-slate-200 p-6">
          <div class="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-navy-950 text-lg">
            {{ feature.icon }}
          </div>
          <h2 class="font-semibold text-navy-950">{{ feature.title }}</h2>
          <p class="mt-2 text-sm text-slate-500">{{ feature.description }}</p>
        </div>
      </div>
    </section>

    <section class="bg-soft-50 px-6 py-16">
      <div class="mx-auto max-w-6xl">
        <h2 class="text-2xl font-bold text-navy-950">지원을 시작할 수 있는 국가</h2>
        <p class="mt-2 text-sm text-slate-500">
          Living Abroad는 현재 가장 이주 수요가 높은 3개국을 우선 지원합니다.
        </p>

        <div class="mt-8 grid gap-6 sm:grid-cols-3">
          <div v-for="country in COUNTRIES" :key="country.code" class="overflow-hidden rounded-xl border border-slate-200 bg-white">
            <div :class="['relative flex h-32 items-end bg-gradient-to-br p-4', country.gradient]">
              <span class="rounded bg-white/90 px-2 py-1 text-xs font-semibold text-navy-950">
                {{ country.flag }} {{ country.nameEn }}
              </span>
            </div>
            <div class="p-5">
              <h3 class="font-semibold text-navy-950">{{ country.nameKo }}</h3>
              <p class="mt-2 text-sm text-slate-500">{{ country.description }}</p>
              <div class="mt-4 flex items-center justify-between text-sm">
                <span class="text-slate-400">정보 기준일: {{ country.lastVerifiedAt }}</span>
                <RouterLink :to="authStore.token ? '/analysis/step-1' : '/auth?tab=login'" class="font-semibold text-navy-950">
                  AI 분석하기 →
                </RouterLink>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-8">
          <DisclaimerBox
            text="적합도 점수는 실제 비자 승인 확률이 아니며 서비스 내부 분석 점수입니다. 공식적인 법률 자문은 이민 변호사 또는 공인 대행인을 통해 받으시길 권장합니다."
          />
        </div>
      </div>
    </section>

    <section class="px-6 py-16 text-center">
      <h2 class="text-2xl font-bold text-navy-950">지금 바로 당신의 해외 생활 가능성을 확인하세요</h2>
      <p class="mt-2 text-sm text-slate-500">가입 후 3분이면 AI가 분석한 개인 맞춤형 리포트를 받아볼 수 있습니다.</p>
      <button
        class="mt-6 rounded-lg bg-navy-950 px-8 py-3 font-semibold text-white hover:bg-navy-900"
        @click="startAnalysis"
      >
        무료 분석 시작하기
      </button>
    </section>
  </div>
</template>
