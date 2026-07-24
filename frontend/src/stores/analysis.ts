import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiClient } from '@/api/client'

interface Step1Data {
  age: number | null
  education: string | null
  major: string
  occupation: string
  experienceYears: number | null
  languageTest: string | null
  languageScore: number | null
}

interface Step2Data {
  careerText: string
  fundsRange: string | null
  familyAccompanied: boolean
  preferredCountry: string | null
}

const emptyStep1 = (): Step1Data => ({
  age: null,
  education: null,
  major: '',
  occupation: '',
  experienceYears: null,
  languageTest: null,
  languageScore: null,
})

const emptyStep2 = (): Step2Data => ({
  careerText: '',
  fundsRange: null,
  familyAccompanied: false,
  preferredCountry: null,
})

export const useAnalysisStore = defineStore('analysis', () => {
  const step1Data = ref<Step1Data>(emptyStep1())
  const step2Data = ref<Step2Data>(emptyStep2())

  const analysisId = ref<number | null>(null)
  const analysisResult = ref<Record<string, unknown> | null>(null)

  function saveStep1(data: Step1Data) {
    step1Data.value = data
  }

  function saveStep2(data: Step2Data) {
    step2Data.value = data
  }

  async function submitAnalysis() {
    const { data } = await apiClient.post('/api/analyses', {
      ...step1Data.value,
      ...step2Data.value,
    })
    analysisId.value = data.analysisId
    return data
  }

  async function loadResult(id: number) {
    const { data } = await apiClient.get(`/api/analyses/${id}`)
    analysisResult.value = data
    return data
  }

  async function createShareLink(id: number) {
    const { data } = await apiClient.post(`/api/analyses/${id}/share`, {})
    return data.shareToken as string
  }

  async function revokeShareLink(id: number) {
    await apiClient.delete(`/api/analyses/${id}/share`)
  }

  async function loadSharedResult(shareToken: string) {
    const { data } = await apiClient.get(`/api/public/analyses/shared/${shareToken}`)
    return data
  }

  function resetAnalysis() {
    step1Data.value = emptyStep1()
    step2Data.value = emptyStep2()
    analysisId.value = null
    analysisResult.value = null
  }

  return {
    step1Data,
    step2Data,
    analysisId,
    analysisResult,
    saveStep1,
    saveStep2,
    submitAnalysis,
    loadResult,
    createShareLink,
    revokeShareLink,
    loadSharedResult,
    resetAnalysis,
  }
})
