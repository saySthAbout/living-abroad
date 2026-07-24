<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { apiClient, getErrorMessage } from '@/api/client'

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

interface ChatSessionSummary {
  sessionId: number
  sessionTitle: string
  countryCode: 'CAN' | 'AUS' | 'GBR' | null
  createdAt: string
  updatedAt: string
}

const { t } = useI18n()

const countryCode = ref<'CAN' | 'AUS' | 'GBR'>('CAN')
const question = ref('')
const errorMessage = ref('')
const sending = ref(false)
const scrollAnchor = ref<HTMLElement | null>(null)
const sessionId = ref<number | null>(null)

const countryFlags: Record<string, string> = { CAN: '🇨🇦', AUS: '🇦🇺', GBR: '🇬🇧' }
function countryFlag(code: string | null) {
  return code ? (countryFlags[code] ?? '🌐') : '🌐'
}

function greetingMessage(): ChatMessage {
  return {
    role: 'ASSISTANT',
    content: t('chat.greeting'),
  }
}

const messages = ref<ChatMessage[]>([greetingMessage()])

const historyOpen = ref(false)
const historyKeyword = ref('')
const historyCountry = ref('')
const historyItems = ref<ChatSessionSummary[]>([])
const historyLoading = ref(false)
const historyError = ref('')

const starterChips = computed(() => [t('chat.starter1'), t('chat.starter2'), t('chat.starter3')])
const followUpChips = computed(() => [t('chat.followUp1'), t('chat.followUp2'), t('chat.followUp3'), t('chat.followUp4')])

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
      sessionId: sessionId.value,
      question: content,
      countryCode: countryCode.value,
    })
    sessionId.value = data.sessionId
    messages.value.push({
      role: 'ASSISTANT',
      content: data.answer,
      answerable: data.answerable,
      sources: data.sources,
    })
  } catch (error) {
    errorMessage.value = getErrorMessage(error, t('chat.errorFallback'))
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

async function toggleHistory() {
  historyOpen.value = !historyOpen.value
  if (historyOpen.value && historyItems.value.length === 0) {
    await searchHistory()
  }
}

async function searchHistory() {
  historyLoading.value = true
  historyError.value = ''
  try {
    const { data } = await apiClient.get('/api/chat/sessions', {
      params: {
        keyword: historyKeyword.value || undefined,
        countryCode: historyCountry.value || undefined,
        page: 0,
        size: 20,
      },
    })
    historyItems.value = data.items ?? []
  } catch (error) {
    historyError.value = getErrorMessage(error, t('chat.historyErrorFallback'))
  } finally {
    historyLoading.value = false
  }
}

async function loadSession(id: number) {
  historyError.value = ''
  try {
    const { data } = await apiClient.get(`/api/chat/sessions/${id}`)
    messages.value = (data.messages ?? []).map(
      (m: { role: 'USER' | 'ASSISTANT'; content: string; answerable?: boolean; sources?: ChatSource[] }) => ({
        role: m.role,
        content: m.content,
        answerable: m.answerable,
        sources: m.sources,
      }),
    )
    sessionId.value = id
    historyOpen.value = false
    await scrollToBottom()
  } catch (error) {
    historyError.value = getErrorMessage(error, t('chat.historyErrorFallback'))
  }
}

function startNewChat() {
  sessionId.value = null
  messages.value = [greetingMessage()]
  historyOpen.value = false
}
</script>

<template>
  <section class="mx-auto max-w-3xl px-6 py-10">
    <div class="flex items-start gap-3 rounded-lg border border-soft-100 bg-soft-50 px-4 py-3 text-sm">
      <span aria-hidden="true">ⓘ</span>
      <div>
        <p class="font-semibold text-navy-950">{{ t('chat.expertTitle') }}</p>
        <p class="mt-1 text-slate-500">
          {{ t('chat.expertDesc') }}
        </p>
        <p class="mt-1 text-xs text-slate-400">{{ t('chat.expertNote') }}</p>
      </div>
    </div>

    <div class="mt-4 flex flex-wrap items-center justify-between gap-2">
      <select v-model="countryCode" class="rounded-lg border border-slate-300 px-3 py-2 text-sm">
        <option value="CAN">{{ t('step2.countryCan') }}</option>
        <option value="AUS">{{ t('step2.countryAus') }}</option>
        <option value="GBR">{{ t('step2.countryGbr') }}</option>
      </select>
      <div class="flex gap-2">
        <button
          type="button"
          class="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-600 hover:border-navy-700"
          @click="startNewChat"
        >
          {{ t('chat.newChat') }}
        </button>
        <button
          type="button"
          class="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-600 hover:border-navy-700"
          @click="toggleHistory"
        >
          {{ t('chat.history') }}
        </button>
      </div>
    </div>

    <div v-if="historyOpen" class="mt-3 rounded-xl border border-slate-200 p-4">
      <div class="flex flex-wrap gap-2">
        <input
          v-model="historyKeyword"
          type="text"
          :placeholder="t('chat.searchPlaceholder')"
          class="min-w-0 flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-navy-700 focus:outline-none"
          @keyup.enter="searchHistory"
        />
        <select v-model="historyCountry" class="rounded-lg border border-slate-300 px-3 py-2 text-sm">
          <option value="">{{ t('chat.allCountries') }}</option>
          <option value="CAN">{{ t('step2.countryCan') }}</option>
          <option value="AUS">{{ t('step2.countryAus') }}</option>
          <option value="GBR">{{ t('step2.countryGbr') }}</option>
        </select>
        <button
          type="button"
          class="rounded-lg bg-navy-950 px-4 py-2 text-sm font-semibold text-white hover:bg-navy-900"
          @click="searchHistory"
        >
          {{ t('chat.search') }}
        </button>
      </div>

      <p v-if="historyLoading" class="mt-3 text-xs text-slate-400">{{ t('chat.loadingHistory') }}</p>
      <p v-else-if="historyError" class="mt-3 text-xs text-red-600">{{ historyError }}</p>
      <p v-else-if="historyItems.length === 0" class="mt-3 text-xs text-slate-400">{{ t('chat.noHistory') }}</p>
      <ul v-else class="mt-3 max-h-64 space-y-1 overflow-y-auto">
        <li v-for="item in historyItems" :key="item.sessionId">
          <button
            type="button"
            class="flex w-full items-center justify-between gap-2 rounded-lg px-3 py-2 text-left text-sm hover:bg-soft-50"
            :class="item.sessionId === sessionId ? 'bg-soft-50' : ''"
            @click="loadSession(item.sessionId)"
          >
            <span class="truncate">{{ countryFlag(item.countryCode) }} {{ item.sessionTitle }}</span>
            <span class="shrink-0 text-xs text-slate-400">{{ item.updatedAt?.slice(0, 10) }}</span>
          </button>
        </li>
      </ul>
    </div>

    <div class="mt-4 max-h-[480px] space-y-4 overflow-y-auto rounded-xl border border-slate-200 p-5">
      <template v-for="(message, index) in messages" :key="index">
        <div v-if="message.role === 'ASSISTANT'" class="max-w-[85%]">
          <p class="mb-1 flex items-center gap-1 text-xs font-semibold text-navy-950">{{ t('chat.assistantName') }}</p>
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
              <span v-if="message.answerable === false" class="rounded bg-amber-50 px-1.5 py-0.5 text-amber-600">{{ t('chat.noEvidence') }}</span>
              <span>{{ t('chat.notLegalAdvice') }}</span>
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
      <p v-if="sending" class="text-xs text-slate-400">{{ t('chat.generating') }}</p>
      <div ref="scrollAnchor" />
    </div>

    <form class="mt-4 flex gap-2" @submit.prevent="sendQuestion()">
      <input
        v-model="question"
        type="text"
        maxlength="1000"
        minlength="2"
        class="flex-1 rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-navy-700 focus:outline-none"
        :placeholder="t('chat.inputPlaceholder')"
      />
      <button type="submit" class="rounded-lg bg-navy-950 px-5 py-2.5 text-sm font-semibold text-white hover:bg-navy-900" :disabled="sending">
        {{ t('chat.send') }}
      </button>
    </form>
    <p v-if="errorMessage" class="mt-2 text-sm text-red-600">{{ errorMessage }}</p>
    <p class="mt-3 text-center text-xs text-slate-400">
      {{ t('chat.footerNote') }}
    </p>
  </section>
</template>
