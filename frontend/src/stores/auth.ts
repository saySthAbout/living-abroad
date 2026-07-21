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
  const user = ref<AuthUser | null>(null)

  function setSession(accessToken: string, authUser: AuthUser) {
    token.value = accessToken
    user.value = authUser
    localStorage.setItem('accessToken', accessToken)
  }

  async function login(payload: LoginPayload) {
    const { data } = await apiClient.post('/api/auth/login', payload)
    setSession(data.accessToken, data.user)
    return data
  }

  async function signup(payload: SignupPayload) {
    const { data } = await apiClient.post('/api/auth/signup', payload)
    setSession(data.accessToken, data.user)
    return data
  }

  async function loadUser() {
    if (!token.value) return
    const { data } = await apiClient.get('/api/users/me')
    user.value = data
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('accessToken')
  }

  return { token, user, login, signup, loadUser, logout }
})
