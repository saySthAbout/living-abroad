import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiClient } from '@/api/client'

interface Step1Data {
  age: number | null
  education: string | null
  major: string
  occupation: string
  experienceYears: number | null
}

interface Step2Data {
  languageTest: string | null
  languageScore: number | null
  fundsRange: string | null
  familyAccompanied: boolean
  preferredCountry: string | null
  careerText: string
}

export const useAnalysisStore = defineStore('analysis', () => {
  const step1Data = ref<Step1Data>({
    age: null,
    education: null,
    major: '',
    occupation: '',
    experienceYears: null,
  })

  const step2Data = ref<Step2Data>({
    languageTest: null,
    languageScore: null,
    fundsRange: null,
    familyAccompanied: false,
    preferredCountry: null,
    careerText: '',
  })

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

  function resetAnalysis() {
    step1Data.value = { age: null, education: null, major: '', occupation: '', experienceYears: null }
    step2Data.value = {
      languageTest: null,
      languageScore: null,
      fundsRange: null,
      familyAccompanied: false,
      preferredCountry: null,
      careerText: '',
    }
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
    resetAnalysis,
  }
})
