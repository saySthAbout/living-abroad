import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'HOME',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/auth',
      name: 'AUTH',
      component: () => import('@/views/AuthView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/analysis/step-1',
      name: 'ANALYSIS_STEP1',
      component: () => import('@/views/AnalysisStep1View.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/analysis/step-2',
      name: 'ANALYSIS_STEP2',
      component: () => import('@/views/AnalysisStep2View.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/analysis/loading',
      name: 'ANALYSIS_LOADING',
      component: () => import('@/views/AnalysisLoadingView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/results/:id',
      name: 'RESULT',
      component: () => import('@/views/AnalysisResultView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/chat',
      name: 'CHAT',
      component: () => import('@/views/AiChatView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/my-results',
      name: 'MY',
      component: () => import('@/views/MyResultsView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.token) {
    return { path: '/auth', query: { tab: 'login' } }
  }
  return true
})

export default router
