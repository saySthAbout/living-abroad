import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiClient } from '@/api/client'

interface AuthUser {
  id: number
  name: string
  email: string
}

interface LoginPayload {
  email: string
  password: string
}

interface SignupPayload {
  name: string
  email: string
  password: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('accessToken'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const user = ref<AuthUser | null>(null)

  function setSession(accessToken: string, newRefreshToken: string, authUser: AuthUser) {
    token.value = accessToken
    refreshToken.value = newRefreshToken
    user.value = authUser
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', newRefreshToken)
  }

  async function login(payload: LoginPayload) {
    const { data } = await apiClient.post('/api/auth/login', payload)
    setSession(data.accessToken, data.refreshToken, data.user)
    return data
  }

  async function signup(payload: SignupPayload) {
    const { data } = await apiClient.post('/api/auth/signup', payload)
    setSession(data.accessToken, data.refreshToken, data.user)
    return data
  }

  async function loadUser() {
    if (!token.value) return
    const { data } = await apiClient.get('/api/users/me')
    user.value = data
  }

  // 액세스 토큰 만료 시 axios 응답 인터셉터가 호출한다. 실패하면 예외를 던져 인터셉터가 logout()으로 넘어가게 한다.
  async function refresh(): Promise<string> {
    if (!refreshToken.value) {
      throw new Error('저장된 refresh token이 없습니다.')
    }
    const { data } = await apiClient.post('/api/auth/refresh', { refreshToken: refreshToken.value })
    setSession(data.accessToken, data.refreshToken, data.user)
    return data.accessToken
  }

  function logout() {
    const rawRefreshToken = refreshToken.value
    token.value = null
    refreshToken.value = null
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')

    if (rawRefreshToken) {
      apiClient.post('/api/auth/logout', { refreshToken: rawRefreshToken }).catch(() => {})
    }
  }

  return { token, refreshToken, user, login, signup, loadUser, refresh, logout }
})
