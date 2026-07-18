<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeTab = computed(() => (route.query.tab === 'signup' ? 'signup' : 'login'))

const loginForm = ref({ email: '', password: '' })
const signupForm = ref({ name: '', email: '', password: '', passwordConfirm: '' })
const errorMessage = ref('')

function switchTab(tab: 'login' | 'signup') {
  errorMessage.value = ''
  router.replace({ query: { tab } })
}

async function submitLogin() {
  errorMessage.value = ''
  try {
    await authStore.login(loginForm.value)
    router.push('/analysis/step-1')
  } catch {
    errorMessage.value = '이메일 또는 비밀번호를 확인해 주세요.'
  }
}

async function submitSignup() {
  errorMessage.value = ''
  if (signupForm.value.password !== signupForm.value.passwordConfirm) {
    errorMessage.value = '비밀번호가 일치하지 않습니다.'
    return
  }
  try {
    await authStore.signup({
      name: signupForm.value.name,
      email: signupForm.value.email,
      password: signupForm.value.password,
    })
    router.push('/analysis/step-1')
  } catch {
    errorMessage.value = '회원가입 처리 중 오류가 발생했습니다.'
  }
}
</script>

<template>
  <section class="flex justify-center bg-soft-50 px-6 py-16">
    <div class="w-full max-w-md rounded-xl border border-slate-200 bg-white p-8">
      <div class="mb-8 flex border-b border-slate-200 text-center">
        <button
          class="flex-1 pb-3 text-sm font-semibold"
          :class="activeTab === 'login' ? 'border-b-2 border-navy-950 text-navy-950' : 'text-slate-400'"
          @click="switchTab('login')"
        >
          로그인
        </button>
        <button
          class="flex-1 pb-3 text-sm font-semibold"
          :class="activeTab === 'signup' ? 'border-b-2 border-navy-950 text-navy-950' : 'text-slate-400'"
          @click="switchTab('signup')"
        >
          회원가입
        </button>
      </div>

      <form v-if="activeTab === 'login'" class="space-y-5" @submit.prevent="submitLogin">
        <label class="block">
          <span class="text-sm font-medium text-navy-950">이메일 주소</span>
          <input
            v-model="loginForm.email"
            type="email"
            placeholder="example@livingabroad.com"
            class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
            required
          />
        </label>
        <label class="block">
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium text-navy-950">비밀번호</span>
            <span class="text-xs text-navy-700">비밀번호 찾기</span>
          </div>
          <input
            v-model="loginForm.password"
            type="password"
            placeholder="••••••••"
            class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
            required
          />
        </label>
        <button type="submit" class="w-full rounded-lg bg-navy-950 py-3 text-sm font-semibold text-white hover:bg-navy-900">
          로그인
        </button>
      </form>

      <form v-else class="space-y-5" @submit.prevent="submitSignup">
        <label class="block">
          <span class="text-sm font-medium text-navy-950">이름</span>
          <input
            v-model="signupForm.name"
            type="text"
            placeholder="홍길동"
            class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
            required
          />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">이메일 주소</span>
          <input
            v-model="signupForm.email"
            type="email"
            placeholder="example@livingabroad.com"
            class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
            required
          />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">비밀번호</span>
          <input
            v-model="signupForm.password"
            type="password"
            placeholder="8자 이상 입력"
            minlength="8"
            class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
            required
          />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-navy-950">비밀번호 확인</span>
          <input
            v-model="signupForm.passwordConfirm"
            type="password"
            placeholder="비밀번호 재입력"
            class="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
            required
          />
        </label>
        <button type="submit" class="w-full rounded-lg bg-navy-950 py-3 text-sm font-semibold text-white hover:bg-navy-900">
          회원가입 완료
        </button>
      </form>

      <p v-if="errorMessage" class="mt-4 text-center text-sm text-red-600">{{ errorMessage }}</p>
      <p class="mt-4 text-center text-xs text-slate-400">정확한 정보를 입력하여 안전한 이민 준비를 시작하세요.</p>
    </div>
  </section>
</template>
