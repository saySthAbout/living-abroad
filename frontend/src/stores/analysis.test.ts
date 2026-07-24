import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAnalysisStore } from '@/stores/analysis'
import { apiClient } from '@/api/client'

vi.mock('@/api/client', () => ({
  apiClient: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

describe('useAnalysisStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('submitAnalysis posts the merged step1+step2 payload and stores the analysisId', async () => {
    const store = useAnalysisStore()
    store.saveStep1({
      age: 30,
      education: 'BACHELOR',
      major: '컴퓨터공학',
      occupation: '소프트웨어 엔지니어',
      experienceYears: 5,
      languageTest: 'IELTS_GENERAL',
      languageScore: 7,
    })
    store.saveStep2({
      careerText: '5년간 백엔드 개발자로 근무했습니다.',
      fundsRange: '30M_50M',
      familyAccompanied: true,
      preferredCountry: 'CAN',
    })
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { analysisId: 42, status: 'PENDING' } })

    const result = await store.submitAnalysis()

    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/analyses',
      expect.objectContaining({
        age: 30,
        occupation: '소프트웨어 엔지니어',
        careerText: '5년간 백엔드 개발자로 근무했습니다.',
        fundsRange: '30M_50M',
        preferredCountry: 'CAN',
      }),
    )
    expect(store.analysisId).toBe(42)
    expect(result.status).toBe('PENDING')
  })

  it('loadResult fetches and stores the analysis result', async () => {
    const store = useAnalysisStore()
    const payload = { analysisId: 7, status: 'COMPLETED', results: [] }
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: payload })

    const result = await store.loadResult(7)

    expect(apiClient.get).toHaveBeenCalledWith('/api/analyses/7')
    expect(store.analysisResult).toEqual(payload)
    expect(result).toEqual(payload)
  })

  it('resetAnalysis clears step data, analysisId, and result back to defaults', () => {
    const store = useAnalysisStore()
    store.saveStep1({
      age: 30,
      education: 'BACHELOR',
      major: '',
      occupation: '',
      experienceYears: 5,
      languageTest: null,
      languageScore: null,
    })
    store.saveStep2({ careerText: 'x', fundsRange: '30M_50M', familyAccompanied: true, preferredCountry: 'CAN' })
    store.analysisId = 1
    store.analysisResult = { foo: 'bar' }

    store.resetAnalysis()

    expect(store.step1Data.age).toBeNull()
    expect(store.step2Data.careerText).toBe('')
    expect(store.step2Data.familyAccompanied).toBe(false)
    expect(store.analysisId).toBeNull()
    expect(store.analysisResult).toBeNull()
  })
})
