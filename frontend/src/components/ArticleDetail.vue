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
  <main class="detail-page">
    <div v-if="!article" class="empty-detail">
      <span>記事を選択すると詳細を編集できます。</span>
    </div>

    <form v-else class="detail-form" @submit.prevent="submit">
      <div class="detail-topbar">
        <button type="button" class="ghost-button compact-button" @click="emit('back')">
          <ArrowLeft :size="17" />
          戻る
        </button>
        <div class="detail-topbar-actions">
          <button type="button" class="icon-button" title="お気に入り" @click="form.favorite = !form.favorite">
            <Heart :size="19" :fill="form.favorite ? 'currentColor' : 'none'" />
          </button>
          <button type="submit" class="primary-button compact-button">
            <Save :size="17" />
            保存
          </button>
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
            <textarea v-model="form.summary" rows="5" />
          </label>

          <label>
            タグ
            <input v-model="tagText" placeholder="React, Spring Boot" />
          </label>

          <label>
            メモ
            <textarea v-model="form.notes" rows="8" />
          </label>
        </section>

        <aside class="detail-meta">
          <div class="field-row stacked">
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

          <div class="detail-actions">
            <button type="button" class="danger-button" @click="emit('delete', article.id)">
              <Trash2 :size="17" />
              削除
            </button>
          </div>
        </aside>
      </div>
    </form>
  </main>
</template>
