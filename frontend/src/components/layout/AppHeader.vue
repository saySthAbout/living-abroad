<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<template>
  <header class="border-b border-slate-200 bg-white">
    <div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
      <RouterLink to="/" class="text-lg font-bold text-navy-950">Living Abroad</RouterLink>
      <nav class="hidden items-center gap-8 text-sm font-medium text-slate-500 sm:flex">
        <RouterLink to="/" class="hover:text-navy-950 [&.router-link-exact-active]:font-semibold [&.router-link-exact-active]:text-navy-950">홈</RouterLink>
        <RouterLink to="/analysis/step-1" class="hover:text-navy-950 [&.router-link-active]:font-semibold [&.router-link-active]:text-navy-950">AI 분석</RouterLink>
        <RouterLink to="/chat" class="hover:text-navy-950 [&.router-link-active]:font-semibold [&.router-link-active]:text-navy-950">AI 상담</RouterLink>
        <RouterLink to="/my-results" class="hover:text-navy-950 [&.router-link-active]:font-semibold [&.router-link-active]:text-navy-950">내 결과</RouterLink>
      </nav>
      <div class="flex items-center gap-3">
        <template v-if="!authStore.token">
          <RouterLink to="/auth?tab=login" class="text-sm font-medium text-slate-600 hover:text-navy-950">로그인</RouterLink>
          <RouterLink
            to="/auth?tab=signup"
            class="rounded-lg bg-navy-950 px-4 py-2 text-sm font-semibold text-white hover:bg-navy-900"
          >
            회원가입
          </RouterLink>
        </template>
        <button
          v-else
          class="text-sm font-medium text-slate-600 hover:text-navy-950"
          @click="handleLogout"
        >
          로그아웃
        </button>
      </div>
    </div>
  </header>
</template>
