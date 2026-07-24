import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { apiClient } from '@/api/client'

vi.mock('@/api/client', () => ({
  apiClient: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

describe('useAuthStore', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('login stores the session and persists tokens to localStorage', async () => {
    const user = { id: 1, name: '김진아', email: 'jinah@example.com', emailVerified: false }
    vi.mocked(apiClient.post).mockResolvedValueOnce({
      data: { accessToken: 'access-1', refreshToken: 'refresh-1', user },
    })

    const store = useAuthStore()
    const result = await store.login({ email: user.email, password: 'pw' })

    expect(apiClient.post).toHaveBeenCalledWith('/api/auth/login', { email: user.email, password: 'pw' })
    expect(store.token).toBe('access-1')
    expect(store.refreshToken).toBe('refresh-1')
    expect(store.user).toEqual(user)
    expect(localStorage.getItem('accessToken')).toBe('access-1')
    expect(localStorage.getItem('refreshToken')).toBe('refresh-1')
    expect(result.user).toEqual(user)
  })

  it('signup stores the session the same way as login', async () => {
    const user = { id: 2, name: '홍길동', email: 'hong@example.com', emailVerified: false }
    vi.mocked(apiClient.post).mockResolvedValueOnce({
      data: { accessToken: 'access-2', refreshToken: 'refresh-2', user },
    })

    const store = useAuthStore()
    await store.signup({ name: user.name, email: user.email, password: 'pw' })

    expect(store.token).toBe('access-2')
    expect(store.user).toEqual(user)
  })

  it('loadUser does nothing when there is no token', async () => {
    const store = useAuthStore()
    await store.loadUser()
    expect(apiClient.get).not.toHaveBeenCalled()
  })

  it('loadUser fetches and stores the current user when a token exists', async () => {
    const store = useAuthStore()
    store.token = 'access-1'
    const user = { id: 1, name: '김진아', email: 'jinah@example.com', emailVerified: true }
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: user })

    await store.loadUser()

    expect(apiClient.get).toHaveBeenCalledWith('/api/users/me')
    expect(store.user).toEqual(user)
  })

  it('refresh throws without calling the API when there is no refresh token', async () => {
    const store = useAuthStore()
    store.refreshToken = null

    await expect(store.refresh()).rejects.toThrow('저장된 refresh token이 없습니다.')
    expect(apiClient.post).not.toHaveBeenCalled()
  })

  it('refresh rotates tokens and returns the new access token', async () => {
    const store = useAuthStore()
    store.refreshToken = 'refresh-old'
    const user = { id: 1, name: '김진아', email: 'jinah@example.com', emailVerified: true }
    vi.mocked(apiClient.post).mockResolvedValueOnce({
      data: { accessToken: 'access-new', refreshToken: 'refresh-new', user },
    })

    const newAccessToken = await store.refresh()

    expect(apiClient.post).toHaveBeenCalledWith('/api/auth/refresh', { refreshToken: 'refresh-old' })
    expect(newAccessToken).toBe('access-new')
    expect(store.token).toBe('access-new')
    expect(store.refreshToken).toBe('refresh-new')
  })

  it('logout clears local state and fires a best-effort logout request', async () => {
    const store = useAuthStore()
    store.token = 'access-1'
    store.refreshToken = 'refresh-1'
    store.user = { id: 1, name: '김진아', email: 'jinah@example.com', emailVerified: true }
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: {} })

    store.logout()

    expect(store.token).toBeNull()
    expect(store.refreshToken).toBeNull()
    expect(store.user).toBeNull()
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
    expect(apiClient.post).toHaveBeenCalledWith('/api/auth/logout', { refreshToken: 'refresh-1' })
  })

  it('logout does not throw even if the best-effort request rejects', () => {
    const store = useAuthStore()
    store.token = 'access-1'
    store.refreshToken = 'refresh-1'
    vi.mocked(apiClient.post).mockRejectedValueOnce(new Error('network down'))

    expect(() => store.logout()).not.toThrow()
    expect(store.token).toBeNull()
  })

  it('logout does not call the API when there was no refresh token', () => {
    const store = useAuthStore()
    store.token = null
    store.refreshToken = null

    store.logout()

    expect(apiClient.post).not.toHaveBeenCalled()
  })
})
