<script setup>
import { computed, reactive, watch } from 'vue'
import { ArrowLeft, ExternalLink, Heart, Save, Trash2 } from 'lucide-vue-next'

const props = defineProps({
  article: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['back', 'save', 'delete'])

const form = reactive(emptyForm())
const statusOptions = [
  { label: '未読', value: 'UNREAD' },
  { label: '読了', value: 'READ' }
]

const tagText = computed({
  get: () => form.tags.join(', '),
  set: (value) => {
    form.tags = value.split(',').map((tag) => tag.trim()).filter(Boolean)
  }
})

watch(
  () => props.article,
  (article) => {
    Object.assign(form, article ? toForm(article) : emptyForm())
  },
  { immediate: true }
)

function emptyForm() {
  return {
    id: '',
    url: '',
    title: '',
    summary: '',
    status: 'UNREAD',
    readDate: null,
    favorite: false,
    notes: '',
    tags: []
  }
}

function toForm(article) {
  return {
    id: article.id,
    url: article.url,
    title: article.title,
    summary: article.summary || '',
    status: article.status || 'UNREAD',
    readDate: article.readDate || null,
    favorite: article.favorite,
    notes: article.notes || '',
    tags: article.tags?.map((tag) => tag.name) || []
  }
}

function submit() {
  emit('save', { ...form, readDate: form.readDate || null })
}
</script>

<template>
  <main class="detail-page">
    <div v-if="!article" class="empty-detail">
      <span>記事を選択すると詳細を編集できます。</span>
    </div>

    <VForm v-else class="detail-form" @submit.prevent="submit">
      <div class="detail-topbar">
        <VBtn class="compact-button" variant="outlined" @click="emit('back')">
          <template #prepend>
            <ArrowLeft :size="17" />
          </template>
          戻る
        </VBtn>
        <div class="detail-topbar-actions">
          <VBtn icon :color="form.favorite ? 'primary' : undefined" title="お気に入り" @click="form.favorite = !form.favorite">
            <Heart :size="19" :fill="form.favorite ? 'currentColor' : 'none'" />
          </VBtn>
          <VBtn color="primary" type="submit" class="compact-button">
            <template #prepend>
              <Save :size="17" />
            </template>
            保存
          </VBtn>
        </div>
      </div>

      <div class="detail-header">
        <div>
          <p class="eyebrow">Article Detail</p>
          <h2>{{ article.title }}</h2>
          <a :href="article.url" target="_blank" rel="noreferrer">
            <ExternalLink :size="15" />
            {{ article.url }}
          </a>
        </div>
      </div>

      <div class="detail-layout">
        <section class="detail-main">
          <VTextField v-model="form.title" label="タイトル" required />

          <VTextField v-model="form.url" label="URL" type="url" required />

          <VTextarea v-model="form.summary" label="概要" rows="5" auto-grow />

          <VTextField v-model="tagText" label="タグ" placeholder="React, Spring Boot" />

          <VTextarea v-model="form.notes" label="メモ" rows="8" auto-grow />
        </section>

        <VCard class="detail-meta" variant="tonal">
          <VCardText class="detail-meta-content">
            <div class="field-row stacked">
              <VSelect v-model="form.status" label="ステータス" :items="statusOptions" item-title="label" item-value="value" />
              <VTextField v-model="form.readDate" label="読了日" type="date" clearable />
            </div>

            <div class="detail-actions">
              <VBtn color="error" variant="outlined" @click="emit('delete', article.id)">
                <template #prepend>
                  <Trash2 :size="17" />
                </template>
                削除
              </VBtn>
            </div>
          </VCardText>
        </VCard>
      </div>
    </VForm>
  </main>
</template>
