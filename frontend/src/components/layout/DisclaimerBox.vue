<script setup lang="ts">
withDefaults(defineProps<{ text?: string }>(), {
  text: '본 서비스의 점수는 실제 비자 승인 확률이 아니며, AI 상담은 법률 자문이 아닙니다.',
})
</script>

<template>
  <p class="rounded bg-gray-50 px-4 py-2 text-xs text-gray-500">{{ text }}</p>
</template>
