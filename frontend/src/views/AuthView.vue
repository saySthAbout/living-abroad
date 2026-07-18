<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeTab = computed(() => (route.query.tab === 'signup' ? 'signup' : 'login'))

const loginForm = ref({ email: '', password: '' })
const signupForm = ref({ name: '', email: '', password: '' })
const errorMessage = ref('')

function switchTab(tab: 'login' | 'signup') {
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
  try {
    await authStore.signup(signupForm.value)
    router.push('/analysis/step-1')
  } catch {
    errorMessage.value = '회원가입 처리 중 오류가 발생했습니다.'
  }
}
</script>

<template>
  <section class="mx-auto max-w-sm">
    <div class="mb-6 flex border-b border-gray-200">
      <button
        class="flex-1 py-2"
        :class="activeTab === 'login' ? 'border-b-2 border-gray-900 font-semibold' : 'text-gray-400'"
        @click="switchTab('login')"
      >
        로그인
      </button>
      <button
        class="flex-1 py-2"
        :class="activeTab === 'signup' ? 'border-b-2 border-gray-900 font-semibold' : 'text-gray-400'"
        @click="switchTab('signup')"
      >
        회원가입
      </button>
    </div>

    <form v-if="activeTab === 'login'" class="space-y-4" @submit.prevent="submitLogin">
      <input v-model="loginForm.email" type="email" placeholder="이메일" class="w-full rounded border border-gray-300 px-3 py-2" required />
      <input v-model="loginForm.password" type="password" placeholder="비밀번호" class="w-full rounded border border-gray-300 px-3 py-2" required />
      <button type="submit" class="w-full rounded bg-gray-900 py-2 text-white">로그인</button>
    </form>

    <form v-else class="space-y-4" @submit.prevent="submitSignup">
      <input v-model="signupForm.name" type="text" placeholder="이름" class="w-full rounded border border-gray-300 px-3 py-2" required />
      <input v-model="signupForm.email" type="email" placeholder="이메일" class="w-full rounded border border-gray-300 px-3 py-2" required />
      <input v-model="signupForm.password" type="password" placeholder="비밀번호" class="w-full rounded border border-gray-300 px-3 py-2" required />
      <button type="submit" class="w-full rounded bg-gray-900 py-2 text-white">회원가입</button>
    </form>

    <p v-if="errorMessage" class="mt-4 text-sm text-red-600">{{ errorMessage }}</p>
  </section>
</template>
