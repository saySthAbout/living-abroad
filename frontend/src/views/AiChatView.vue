<script setup lang="ts">
import { ref } from 'vue'
import { apiClient } from '@/api/client'
import DisclaimerBox from '@/components/layout/DisclaimerBox.vue'

interface ChatMessage {
  role: 'USER' | 'ASSISTANT'
  content: string
}

const countryCode = ref('CAN')
const question = ref('')
const messages = ref<ChatMessage[]>([])
const errorMessage = ref('')

async function sendQuestion() {
  if (!question.value.trim()) return
  errorMessage.value = ''
  const currentQuestion = question.value
  messages.value.push({ role: 'USER', content: currentQuestion })
  question.value = ''
  try {
    const { data } = await apiClient.post('/api/chat', {
      question: currentQuestion,
      countryCode: countryCode.value,
    })
    messages.value.push({ role: 'ASSISTANT', content: data.answer })
  } catch {
    errorMessage.value = '답변 생성 중 오류가 발생했습니다.'
  }
}
</script>

<template>
  <section class="mx-auto max-w-2xl space-y-4">
    <h1 class="text-xl font-semibold">AI 상담</h1>
    <select v-model="countryCode" class="rounded border border-gray-300 px-3 py-2">
      <option value="CAN">캐나다</option>
      <option value="AUS">호주</option>
      <option value="GBR">영국</option>
    </select>

    <div class="min-h-[240px] space-y-3 rounded border border-gray-200 p-4">
      <p
        v-for="(message, index) in messages"
        :key="index"
        :class="message.role === 'USER' ? 'text-right' : 'text-left'"
      >
        <span class="inline-block rounded bg-gray-100 px-3 py-2 text-sm">{{ message.content }}</span>
      </p>
    </div>

    <form class="flex gap-2" @submit.prevent="sendQuestion">
      <input v-model="question" type="text" maxlength="1000" class="flex-1 rounded border border-gray-300 px-3 py-2" placeholder="질문을 입력해 주세요." />
      <button type="submit" class="rounded bg-gray-900 px-4 py-2 text-white">전송</button>
    </form>
    <p v-if="errorMessage" class="text-sm text-red-600">{{ errorMessage }}</p>
    <DisclaimerBox text="본 답변은 법률 자문이 아닙니다." />
  </section>
</template>
