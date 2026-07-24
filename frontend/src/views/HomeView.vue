<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import DisclaimerBox from '@/components/layout/DisclaimerBox.vue'
import { COUNTRIES } from '@/constants/countries'

const router = useRouter()
const authStore = useAuthStore()
const { t, locale } = useI18n()

function startAnalysis() {
  router.push(authStore.token ? '/analysis/step-1' : '/auth?tab=login')
}

function startChat() {
  router.push(authStore.token ? '/chat' : '/auth?tab=login')
}

const features = computed(() => [
  { icon: '🛡️', title: t('home.feature1Title'), description: t('home.feature1Desc') },
  { icon: '💼', title: t('home.feature2Title'), description: t('home.feature2Desc') },
  { icon: '💬', title: t('home.feature3Title'), description: t('home.feature3Desc') },
])
</script>

<template>
  <div>
    <section class="bg-soft-50 px-6 py-20">
      <div class="mx-auto max-w-3xl text-center">
        <h1 class="text-4xl font-extrabold leading-tight text-navy-950 sm:text-5xl">
          {{ t('home.heroTitleLine1') }}<br />{{ t('home.heroTitleLine2') }}
        </h1>
        <p class="mt-6 text-slate-500">
          {{ t('home.heroSubtitle') }}
        </p>
        <div class="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
          <button
            class="rounded-lg bg-gold-500 px-6 py-3 font-semibold text-navy-950 hover:bg-gold-400"
            @click="startAnalysis"
          >
            {{ t('home.startAnalysis') }}
          </button>
          <button
            class="rounded-lg border border-navy-950 px-6 py-3 font-semibold text-navy-950 hover:bg-white"
            @click="startChat"
          >
            {{ t('home.startChat') }}
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
        <h2 class="text-2xl font-bold text-navy-950">{{ t('home.countriesTitle') }}</h2>
        <p class="mt-2 text-sm text-slate-500">
          {{ t('home.countriesSubtitle') }}
        </p>

        <div class="mt-8 grid gap-6 sm:grid-cols-3">
          <div v-for="country in COUNTRIES" :key="country.code" class="overflow-hidden rounded-xl border border-slate-200 bg-white">
            <div :class="['relative flex h-32 items-end bg-gradient-to-br p-4', country.gradient]">
              <span class="rounded bg-white/90 px-2 py-1 text-xs font-semibold text-navy-950">
                {{ country.flag }} {{ country.nameEn }}
              </span>
            </div>
            <div class="p-5">
              <h3 class="font-semibold text-navy-950">{{ locale === 'ko' ? country.nameKo : country.nameEn }}</h3>
              <p class="mt-2 text-sm text-slate-500">{{ locale === 'ko' ? country.description : country.descriptionEn }}</p>
              <div class="mt-4 flex items-center justify-between text-sm">
                <span class="text-slate-400">{{ t('home.verifiedAt', { date: country.lastVerifiedAt }) }}</span>
                <RouterLink :to="authStore.token ? '/analysis/step-1' : '/auth?tab=login'" class="font-semibold text-navy-950">
                  {{ t('home.analyzeCta') }}
                </RouterLink>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-8">
          <DisclaimerBox :text="t('home.countriesDisclaimer')" />
        </div>
      </div>
    </section>

    <section class="px-6 py-16 text-center">
      <h2 class="text-2xl font-bold text-navy-950">{{ t('home.finalCtaTitle') }}</h2>
      <p class="mt-2 text-sm text-slate-500">{{ t('home.finalCtaSubtitle') }}</p>
      <button
        class="mt-6 rounded-lg bg-navy-950 px-8 py-3 font-semibold text-white hover:bg-navy-900"
        @click="startAnalysis"
      >
        {{ t('home.finalCtaButton') }}
      </button>
    </section>
  </div>
</template>
