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
  // /api/auth/** 는 인증이 필요 없는 엔드포인트다. 특히 refresh/logout에 만료되거나
  // 손상된 액세스 토큰을 Authorization 헤더로 실어 보내면, Spring Security의 JWT 필터가
  // permitAll 여부와 무관하게 요청 자체를 401로 거부해 refresh 흐름이 깨진다.
  const isAuthEndpoint = config.url?.startsWith('/api/auth/')
  const authStore = useAuthStore()
  if (authStore.token && !isAuthEndpoint) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

let refreshPromise: Promise<string> | null = null

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const authStore = useAuthStore()
    const originalRequest = error.config

    const shouldAttemptRefresh =
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      originalRequest.url !== '/api/auth/refresh' &&
      !!authStore.refreshToken

    if (shouldAttemptRefresh) {
      originalRequest._retry = true
      try {
        if (!refreshPromise) {
          refreshPromise = authStore.refresh().finally(() => {
            refreshPromise = null
          })
        }
        const newAccessToken = await refreshPromise
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return apiClient(originalRequest)
      } catch {
        // 갱신 실패 시 아래에서 로그아웃 처리
      }
    }

    if (error.response?.status === 401) {
      authStore.logout()
    }
    return Promise.reject(error)
  },
)
