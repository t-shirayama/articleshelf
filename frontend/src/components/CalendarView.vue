<script setup lang="ts">
import { computed, ref } from 'vue'
import { CalendarDays, CheckCircle2, ChevronLeft, ChevronRight, PlusCircle } from 'lucide-vue-next'
import type { Article } from '../types'

type CalendarMode = 'created' | 'read'

interface CalendarCell {
  key: string
  label: number | null
  date: string
  weekday: number | null
  articles: Article[]
  outside: boolean
}

const props = defineProps<{
  articles: Article[]
}>()

const emit = defineEmits<{
  'open-article': [article: Article]
}>()

const mode = ref<CalendarMode>('created')
const visibleMonth = ref(startOfMonth(new Date()))
const weekdays = [
  { label: '日', type: 'sunday' },
  { label: '月', type: 'weekday' },
  { label: '火', type: 'weekday' },
  { label: '水', type: 'weekday' },
  { label: '木', type: 'weekday' },
  { label: '金', type: 'weekday' },
  { label: '土', type: 'saturday' }
]

const monthLabel = computed(() => {
  const year = visibleMonth.value.getFullYear()
  const month = visibleMonth.value.getMonth() + 1
  return `${year}年${month}月`
})

const monthStartKey = computed(() => toDateKey(visibleMonth.value))
const monthEndKey = computed(() => toDateKey(new Date(visibleMonth.value.getFullYear(), visibleMonth.value.getMonth() + 1, 0)))

const createdInMonth = computed(() => props.articles.filter((article) => isInVisibleMonth(dateKey(article.createdAt))))
const readInMonth = computed(() => props.articles.filter((article) => isInVisibleMonth(dateKey(article.readDate))))
const backlogDelta = computed(() => createdInMonth.value.length - readInMonth.value.length)

const calendarCells = computed<CalendarCell[]>(() => {
  const year = visibleMonth.value.getFullYear()
  const month = visibleMonth.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const cells: CalendarCell[] = []

  for (let index = 0; index < firstDay.getDay(); index += 1) {
    cells.push({
      key: `blank-start-${index}`,
      label: null,
      date: '',
      weekday: null,
      articles: [],
      outside: true
    })
  }

  for (let day = 1; day <= lastDay.getDate(); day += 1) {
    const date = new Date(year, month, day)
    const key = toDateKey(date)
    cells.push({
      key,
      label: day,
      date: key,
      weekday: date.getDay(),
      articles: articlesForDate(key),
      outside: false
    })
  }

  while (cells.length < 42) {
    cells.push({
      key: `blank-end-${cells.length}`,
      label: null,
      date: '',
      weekday: null,
      articles: [],
      outside: true
    })
  }

  return cells
})

function articlesForDate(key: string): Article[] {
  return props.articles.filter((article) => {
    const targetDate = mode.value === 'created' ? dateKey(article.createdAt) : dateKey(article.readDate)
    return targetDate === key
  })
}

function moveMonth(offset: number): void {
  visibleMonth.value = new Date(visibleMonth.value.getFullYear(), visibleMonth.value.getMonth() + offset, 1)
}

function isInVisibleMonth(key: string): boolean {
  return Boolean(key) && key >= monthStartKey.value && key <= monthEndKey.value
}

function dateKey(value?: string | null): string {
  return value ? value.slice(0, 10) : ''
}

function toDateKey(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}
</script>

<template>
  <section class="calendar-view">
    <header class="calendar-header">
      <div>
        <h1>カレンダー</h1>
      </div>
    </header>

    <div class="calendar-toolbar">
      <VBtnToggle v-model="mode" class="mode-toggle calendar-mode-toggle" mandatory>
        <VBtn value="created">
          <template #prepend>
            <PlusCircle :size="16" />
          </template>
          追加日
        </VBtn>
        <VBtn value="read">
          <template #prepend>
            <CheckCircle2 :size="16" />
          </template>
          読了日
        </VBtn>
      </VBtnToggle>

      <div class="calendar-month-controls">
        <VBtn class="action-button action-button-secondary calendar-month-button" variant="outlined" @click="moveMonth(-1)">
          <template #prepend>
            <ChevronLeft :size="17" />
          </template>
          前月
        </VBtn>
        <strong class="calendar-month-label">{{ monthLabel }}</strong>
        <VBtn class="action-button action-button-secondary calendar-month-button" variant="outlined" @click="moveMonth(1)">
          次月
          <template #append>
            <ChevronRight :size="17" />
          </template>
        </VBtn>
      </div>

      <div class="calendar-summary">
        <span>
          <span class="calendar-summary-label">追加</span>
          <strong>{{ createdInMonth.length }}</strong>
        </span>
        <span>
          <span class="calendar-summary-label">読了</span>
          <strong>{{ readInMonth.length }}</strong>
        </span>
        <span :class="{ 'is-positive': backlogDelta > 0, 'is-negative': backlogDelta < 0 }">
          <span class="calendar-summary-label">積読差分</span>
          <strong>{{ backlogDelta >= 0 ? '+' : '' }}{{ backlogDelta }}</strong>
        </span>
      </div>
    </div>

    <div class="calendar-grid" role="grid">
      <div
        v-for="weekday in weekdays"
        :key="weekday.label"
        class="calendar-weekday"
        :class="`is-${weekday.type}`"
        role="columnheader"
      >
        {{ weekday.label }}
      </div>

      <div
        v-for="cell in calendarCells"
        :key="cell.key"
        class="calendar-day"
        :class="{
          'is-empty': cell.outside,
          'has-articles': cell.articles.length > 0,
          'is-sunday': cell.weekday === 0,
          'is-saturday': cell.weekday === 6
        }"
        role="gridcell"
      >
        <template v-if="!cell.outside">
          <div class="calendar-day-header">
            <div class="calendar-day-title">
              <span class="calendar-day-number">{{ cell.label }}</span>
            </div>
            <small v-if="cell.articles.length > 0">{{ cell.articles.length }}件</small>
          </div>

          <div class="calendar-day-articles">
            <button
              v-for="article in cell.articles"
              :key="article.id"
              class="calendar-article-link"
              type="button"
              @click="emit('open-article', article)"
            >
              <CalendarDays :size="13" />
              <span>{{ article.title }}</span>
            </button>
          </div>
        </template>
      </div>
    </div>
  </section>
</template>
