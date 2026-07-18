<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<template>
  <header class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
    <RouterLink to="/" class="text-lg font-semibold">Living Abroad</RouterLink>
    <nav class="flex items-center gap-6 text-sm">
      <RouterLink to="/analysis/step-1">AI 분석</RouterLink>
      <RouterLink to="/chat">AI 상담</RouterLink>
      <RouterLink to="/my-results">내 결과</RouterLink>
      <RouterLink v-if="!authStore.token" to="/auth?tab=login">로그인</RouterLink>
      <button v-else @click="authStore.logout()">로그아웃</button>
    </nav>
  </header>
</template>
