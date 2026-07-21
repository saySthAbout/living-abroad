import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

// 백엔드가 GlobalExceptionHandler에서 만들어주는 { message: "..." } 형태의 에러 응답을
// 최대한 그대로 보여준다. 형식이 다르면(네트워크 오류, 예상 못한 응답 등) fallback을 쓴다.
export function getErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message
    if (typeof message === 'string' && message.length > 0) {
      return message
    }
  }
  return fallback
}

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json; charset=UTF-8',
  },
})

apiClient.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
    }
    return Promise.reject(error)
  },
)
