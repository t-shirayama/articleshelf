<script setup>
import { computed, reactive, watch } from 'vue'
import { ExternalLink, Heart, Save, Trash2 } from 'lucide-vue-next'

const props = defineProps({
  article: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['save', 'delete'])

const form = reactive(emptyForm())

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
    readDate: '',
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
    readDate: article.readDate || '',
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
  <aside class="detail-panel">
    <div v-if="!article" class="empty-detail">
      <span>記事を選択すると詳細を編集できます。</span>
    </div>

    <form v-else class="detail-form" @submit.prevent="submit">
      <div class="detail-header">
        <div>
          <p class="eyebrow">Article Detail</p>
          <h2>{{ article.title }}</h2>
          <a :href="article.url" target="_blank" rel="noreferrer">
            <ExternalLink :size="15" />
            {{ article.url }}
          </a>
        </div>
        <button type="button" class="icon-button" title="お気に入り" @click="form.favorite = !form.favorite">
          <Heart :size="19" :fill="form.favorite ? 'currentColor' : 'none'" />
        </button>
      </div>

      <label>
        タイトル
        <input v-model="form.title" required />
      </label>

      <label>
        URL
        <input v-model="form.url" type="url" required />
      </label>

      <label>
        概要
        <textarea v-model="form.summary" rows="3" />
      </label>

      <div class="field-row">
        <label>
          ステータス
          <select v-model="form.status">
            <option value="UNREAD">未読</option>
            <option value="READ">読了</option>
          </select>
        </label>
        <label>
          読了日
          <input v-model="form.readDate" type="date" />
        </label>
      </div>

      <label>
        タグ
        <input v-model="tagText" placeholder="React, Spring Boot" />
      </label>

      <label>
        メモ
        <textarea v-model="form.notes" rows="7" />
      </label>

      <div class="detail-actions">
        <button type="button" class="danger-button" @click="emit('delete', article.id)">
          <Trash2 :size="17" />
          削除
        </button>
        <button type="submit" class="primary-button">
          <Save :size="17" />
          保存
        </button>
      </div>
    </form>
  </aside>
</template>
