<script setup lang="ts">
withDefaults(defineProps<{ text?: string }>(), {
  text: '본 서비스의 점수는 실제 비자 승인 확률이 아니며, AI 상담은 법률 자문이 아닙니다.',
})
</script>

<template>
  <p class="flex items-start gap-2 rounded-lg border border-soft-100 bg-soft-50 px-4 py-3 text-xs text-slate-500">
    <span aria-hidden="true">ⓘ</span>
    <span>{{ text }}</span>
  </p>
</template>
