import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { i18n } from '@/i18n'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import { useAnalysisStore } from '@/stores/analysis'
import AnalysisStep1View from './AnalysisStep1View.vue'

vi.mock('@/api/client', () => ({
  apiClient: { get: vi.fn(), post: vi.fn() },
  getErrorMessage: (_error: unknown, fallback: string) => fallback,
}))

describe('AnalysisStep1View', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    useAuthStore().token = 'access-1'
    await router.push('/analysis/step-1')
  })

  it('re-hydrates the form from previously saved step-1 data on mount (e.g. after clicking "이전" from step 2)', async () => {
    useAnalysisStore().saveStep1({
      age: 33,
      education: 'MASTER',
      major: '경영학',
      occupation: '데이터 분석가',
      experienceYears: 6,
      languageTest: 'IELTS_GENERAL',
      languageScore: 7.5,
    })

    const wrapper = mount(AnalysisStep1View, { global: { plugins: [i18n, router] } })
    await flushPromises()

    // DOM order: age, experienceYears, languageScore are the number inputs;
    // major, occupation are the text inputs; education, languageTest are the selects.
    const numberInputs = wrapper.findAll('input[type="number"]')
    const textInputs = wrapper.findAll('input[type="text"]')
    const selects = wrapper.findAll('select')

    expect((numberInputs[0].element as HTMLInputElement).value).toBe('33')
    expect((textInputs[0].element as HTMLInputElement).value).toBe('경영학')
    expect((textInputs[1].element as HTMLInputElement).value).toBe('데이터 분석가')
    expect((numberInputs[1].element as HTMLInputElement).value).toBe('6')
    expect((selects[1].element as HTMLSelectElement).value).toBe('IELTS_GENERAL')
    expect((numberInputs[2].element as HTMLInputElement).value).toBe('7.5')
    expect((selects[0].element as HTMLSelectElement).value).toBe('MASTER')
  })

  it('does not navigate to step 2 when age is out of the allowed range', async () => {
    const wrapper = mount(AnalysisStep1View, { global: { plugins: [i18n, router] } })
    await flushPromises()

    const numberInputs = wrapper.findAll('input[type="number"]')
    const textInputs = wrapper.findAll('input[type="text"]')
    const selects = wrapper.findAll('select')

    await numberInputs[0].setValue(10) // age, below min="18"
    await selects[0].setValue('BACHELOR') // education
    await textInputs[0].setValue('경영학') // major
    await textInputs[1].setValue('데이터 분석가') // occupation
    await numberInputs[1].setValue(6) // experienceYears

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/analysis/step-1')
    expect(wrapper.find('.text-red-600').exists()).toBe(true)
    expect(wrapper.find('.text-red-600').text().length).toBeGreaterThan(0)
  })

  it('navigates to step 2 once all fields are valid', async () => {
    const wrapper = mount(AnalysisStep1View, { global: { plugins: [i18n, router] } })
    await flushPromises()

    const numberInputs = wrapper.findAll('input[type="number"]')
    const textInputs = wrapper.findAll('input[type="text"]')
    const selects = wrapper.findAll('select')

    await numberInputs[0].setValue(30) // age
    await selects[0].setValue('BACHELOR') // education
    await textInputs[0].setValue('경영학') // major
    await textInputs[1].setValue('데이터 분석가') // occupation
    await numberInputs[1].setValue(6) // experienceYears

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    // The next route's component is lazy-loaded; wait for the async navigation to settle.
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/analysis/step-2'))
  })

  it('does not navigate to step 2 when a language test is chosen but no score is entered', async () => {
    const wrapper = mount(AnalysisStep1View, { global: { plugins: [i18n, router] } })
    await flushPromises()

    const numberInputs = wrapper.findAll('input[type="number"]')
    const textInputs = wrapper.findAll('input[type="text"]')
    const selects = wrapper.findAll('select')

    await numberInputs[0].setValue(30) // age
    await selects[0].setValue('BACHELOR') // education
    await textInputs[0].setValue('경영학') // major
    await textInputs[1].setValue('데이터 분석가') // occupation
    await numberInputs[1].setValue(6) // experienceYears
    await selects[1].setValue('IELTS_GENERAL') // languageTest, languageScore left empty

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/analysis/step-1')
    expect(wrapper.find('.text-red-600').exists()).toBe(true)
  })
})
