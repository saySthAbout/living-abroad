<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { apiClient } from '@/api/client'

interface ChatSource {
  title: string
  url: string
  verifiedAt: string
}

interface ChatMessage {
  role: 'USER' | 'ASSISTANT'
  content: string
  answerable?: boolean
  sources?: ChatSource[]
}

const countryCode = ref<'CAN' | 'AUS' | 'GBR'>('CAN')
const question = ref('')
const errorMessage = ref('')
const sending = ref(false)
const scrollAnchor = ref<HTMLElement | null>(null)

const messages = ref<ChatMessage[]>([
  {
    role: 'ASSISTANT',
    content: '안녕하세요! 해외 이주 및 비자 관련하여 궁금한 점을 물어보세요. 데이터에 기반하여 답변해 드립니다.',
  },
])

const starterChips = ['캐나다 Express Entry 기본 자격', '호주 Subclass 189 준비 서류', '영국 Skilled Worker 스폰서 요건']
const followUpChips = ['기본 자격 요건', '준비 필요 서류', '영어 점수 기준', '신청 절차']

async function scrollToBottom() {
  await nextTick()
  scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
}

async function sendQuestion(text?: string) {
  const content = (text ?? question.value).trim()
  if (!content) return
  errorMessage.value = ''
  messages.value.push({ role: 'USER', content })
  question.value = ''
  sending.value = true
  await scrollToBottom()
  try {
    const { data } = await apiClient.post('/api/chat', {
      question: content,
      countryCode: countryCode.value,
    })
    messages.value.push({
      role: 'ASSISTANT',
      content: data.answer,
      answerable: data.answerable,
      sources: data.sources,
    })
  } catch {
    errorMessage.value = '답변 생성 중 오류가 발생했습니다.'
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}
</script>

<template>
  <section class="mx-auto max-w-3xl px-6 py-10">
    <div class="flex items-start gap-3 rounded-lg border border-soft-100 bg-soft-50 px-4 py-3 text-sm">
      <span aria-hidden="true">ⓘ</span>
      <div>
        <p class="font-semibold text-navy-950">이주 전문가 AI 상담</p>
        <p class="mt-1 text-slate-500">
          Living Abroad AI는 캐나다 Express Entry, 호주 Subclass 189, 영국 Skilled Worker Visa에 관한 공식
          정책 정보를 제공합니다.
        </p>
        <p class="mt-1 text-xs text-slate-400">* 타 국가 또는 지원 범위를 벗어난 질문은 정보 제공이 제한될 수 있습니다.</p>
      </div>
    </div>

    <select v-model="countryCode" class="mt-4 rounded-lg border border-slate-300 px-3 py-2 text-sm">
      <option value="CAN">캐나다</option>
      <option value="AUS">호주</option>
      <option value="GBR">영국</option>
    </select>

    <div class="mt-4 max-h-[480px] space-y-4 overflow-y-auto rounded-xl border border-slate-200 p-5">
      <template v-for="(message, index) in messages" :key="index">
        <div v-if="message.role === 'ASSISTANT'" class="max-w-[85%]">
          <p class="mb-1 flex items-center gap-1 text-xs font-semibold text-navy-950">🤖 Living Abroad AI</p>
          <div class="rounded-xl rounded-tl-none bg-soft-50 px-4 py-3 text-sm text-navy-950">
            <p class="whitespace-pre-line">{{ message.content }}</p>
            <div v-if="message.sources?.length" class="mt-3 space-y-1 border-t border-slate-200 pt-2">
              <a
                v-for="source in message.sources"
                :key="source.url"
                :href="source.url"
                target="_blank"
                rel="noopener noreferrer"
                class="block text-xs font-medium text-navy-700 underline"
              >
                🔗 {{ source.title }}
              </a>
            </div>
            <div class="mt-2 flex flex-wrap gap-2 text-[11px] text-slate-400">
              <span v-if="message.answerable === false" class="rounded bg-amber-50 px-1.5 py-0.5 text-amber-600">근거 문서 없음</span>
              <span>본 답변은 법률 자문이 아닙니다.</span>
            </div>
          </div>
          <div v-if="index === 0" class="mt-2 flex flex-wrap gap-2">
            <button
              v-for="chip in starterChips"
              :key="chip"
              class="rounded-full border border-slate-300 px-3 py-1.5 text-xs text-slate-600 hover:border-navy-700"
              @click="sendQuestion(chip)"
            >
              # {{ chip }}
            </button>
          </div>
          <div v-else-if="index === messages.length - 1" class="mt-2 flex flex-wrap gap-2">
            <button
              v-for="chip in followUpChips"
              :key="chip"
              class="rounded-full border border-slate-300 px-3 py-1.5 text-xs text-slate-600 hover:border-navy-700"
              @click="sendQuestion(chip)"
            >
              {{ chip }}
            </button>
          </div>
        </div>
        <div v-else class="flex justify-end">
          <div class="max-w-[85%] rounded-xl rounded-tr-none bg-navy-950 px-4 py-3 text-sm text-white">
            {{ message.content }}
          </div>
        </div>
      </template>
      <p v-if="sending" class="text-xs text-slate-400">답변을 생성하고 있습니다…</p>
      <div ref="scrollAnchor" />
    </div>

    <form class="mt-4 flex gap-2" @submit.prevent="sendQuestion()">
      <input
        v-model="question"
        type="text"
        maxlength="1000"
        minlength="2"
        class="flex-1 rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
        placeholder="이주 계획에 대해 무엇이든 물어보세요..."
      />
      <button type="submit" class="rounded-lg bg-navy-950 px-5 py-2.5 text-sm font-semibold text-white hover:bg-navy-900" :disabled="sending">
        전송 ➤
      </button>
    </form>
    <p v-if="errorMessage" class="mt-2 text-sm text-red-600">{{ errorMessage }}</p>
    <p class="mt-3 text-center text-xs text-slate-400">
      AI가 제공하는 정보는 참고용이며, 최종 결정 전 공식 기관 또는 전문가의 확인을 권장합니다.
    </p>
  </section>
</template>
