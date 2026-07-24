<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <footer class="border-t border-slate-200 bg-soft-50">
    <div class="mx-auto grid max-w-6xl gap-8 px-6 py-10 sm:grid-cols-[2fr_1fr_1fr]">
      <div>
        <p class="text-lg font-bold text-navy-950">Living Abroad</p>
        <p class="mt-2 max-w-sm text-sm text-slate-500">
          {{ t('footer.tagline') }}
        </p>
      </div>
      <div class="text-sm">
        <p class="font-semibold text-navy-950">{{ t('footer.service') }}</p>
        <ul class="mt-3 space-y-2 text-slate-500">
          <li>{{ t('footer.about') }}</li>
          <li>{{ t('footer.terms') }}</li>
        </ul>
      </div>
      <div class="text-sm">
        <p class="font-semibold text-navy-950">{{ t('footer.legal') }}</p>
        <ul class="mt-3 space-y-2 text-slate-500">
          <li>{{ t('footer.disclaimer') }}</li>
          <li>{{ t('footer.privacy') }}</li>
          <li>{{ t('footer.dataAsOf') }}</li>
        </ul>
      </div>
    </div>
    <div class="border-t border-slate-200 px-6 py-4 text-xs text-slate-400">
      {{ t('footer.copyright') }}
    </div>
  </footer>
</template>
