<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <div class="flex items-center justify-center py-8" role="status" :aria-label="t('loadingSpinner.ariaLabel')">
    <div class="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-navy-900" />
  </div>
</template>
