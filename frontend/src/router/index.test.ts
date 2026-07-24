import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

describe('router auth guard', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('redirects to /auth when visiting a requiresAuth route without a token', async () => {
    const authStore = useAuthStore()
    authStore.token = null

    await router.push('/my-results')

    expect(router.currentRoute.value.path).toBe('/auth')
    expect(router.currentRoute.value.query.tab).toBe('login')
  })

  it('allows visiting a requiresAuth route when a token is present', async () => {
    const authStore = useAuthStore()
    authStore.token = 'access-1'

    await router.push('/my-results')

    expect(router.currentRoute.value.path).toBe('/my-results')
  })

  it('allows visiting a public route without a token', async () => {
    const authStore = useAuthStore()
    authStore.token = null

    await router.push('/auth')

    expect(router.currentRoute.value.path).toBe('/auth')
  })
})
