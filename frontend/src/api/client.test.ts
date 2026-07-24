import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import MockAdapter from 'axios-mock-adapter'
import { apiClient, getErrorMessage } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

describe('getErrorMessage', () => {
  it('returns the backend message for an axios error with a message field', () => {
    const error = Object.assign(new Error('network'), {
      isAxiosError: true,
      response: { data: { message: '이미 존재하는 이메일입니다.' } },
    })
    expect(getErrorMessage(error, 'fallback')).toBe('이미 존재하는 이메일입니다.')
  })

  it('returns the fallback when the axios error has no message field', () => {
    const error = Object.assign(new Error('network'), { isAxiosError: true, response: { data: {} } })
    expect(getErrorMessage(error, 'fallback')).toBe('fallback')
  })

  it('returns the fallback for a non-axios error', () => {
    expect(getErrorMessage(new Error('boom'), 'fallback')).toBe('fallback')
  })
})

describe('apiClient interceptors', () => {
  let mock: MockAdapter

  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    mock = new MockAdapter(apiClient)
  })

  afterEach(() => {
    mock.restore()
  })

  it('attaches the Authorization header for non-auth endpoints when a token is set', async () => {
    const authStore = useAuthStore()
    authStore.token = 'access-token-1'
    mock.onGet('/api/users/me').reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access-token-1')
      return [200, { id: 1 }]
    })

    await apiClient.get('/api/users/me')
  })

  it('does not attach Authorization for /api/auth/** endpoints even with a token set', async () => {
    const authStore = useAuthStore()
    authStore.token = 'access-token-1'
    mock.onPost('/api/auth/login').reply((config) => {
      expect(config.headers?.Authorization).toBeUndefined()
      return [200, { accessToken: 'x', refreshToken: 'y', user: {} }]
    })

    await apiClient.post('/api/auth/login', {})
  })

  it('refreshes once and retries on a 401, deduping concurrent requests onto one refresh call', async () => {
    const authStore = useAuthStore()
    authStore.token = 'expired-token'
    authStore.refreshToken = 'valid-refresh-token'

    let refreshCalls = 0
    mock.onPost('/api/auth/refresh').reply(() => {
      refreshCalls += 1
      return [200, { accessToken: 'new-access-token', refreshToken: 'new-refresh-token', user: { id: 1 } }]
    })

    let protectedCalls = 0
    mock.onGet('/api/users/me').reply((config) => {
      protectedCalls += 1
      if (config.headers?.Authorization === 'Bearer expired-token') {
        return [401, { message: 'expired' }]
      }
      return [200, { id: 1 }]
    })

    const [first, second] = await Promise.all([apiClient.get('/api/users/me'), apiClient.get('/api/users/me')])

    expect(refreshCalls).toBe(1)
    expect(protectedCalls).toBe(4)
    expect(first.status).toBe(200)
    expect(second.status).toBe(200)
    expect(authStore.token).toBe('new-access-token')
  })

  it('logs out when refresh itself fails', async () => {
    const authStore = useAuthStore()
    authStore.token = 'expired-token'
    authStore.refreshToken = 'stale-refresh-token'

    mock.onPost('/api/auth/refresh').reply(401, { message: 'invalid refresh token' })
    mock.onPost('/api/auth/logout').reply(200)
    mock.onGet('/api/users/me').reply(401, { message: 'expired' })

    await expect(apiClient.get('/api/users/me')).rejects.toBeTruthy()

    expect(authStore.token).toBeNull()
    expect(authStore.refreshToken).toBeNull()
  })

  it('logs out immediately on a 401 when there is no refresh token to try', async () => {
    const authStore = useAuthStore()
    authStore.token = 'expired-token'
    authStore.refreshToken = null

    mock.onGet('/api/users/me').reply(401, { message: 'expired' })

    await expect(apiClient.get('/api/users/me')).rejects.toBeTruthy()
    expect(authStore.token).toBeNull()
  })
})
