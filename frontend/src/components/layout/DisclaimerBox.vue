<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{ text?: string }>()
const { t } = useI18n()
const displayText = computed(() => props.text ?? t('disclaimer.default'))
</script>

<template>
  <p class="flex items-start gap-2 rounded-lg border border-soft-100 bg-soft-50 px-4 py-3 text-xs text-slate-500">
    <span aria-hidden="true">ⓘ</span>
    <span>{{ displayText }}</span>
  </p>
</template>
