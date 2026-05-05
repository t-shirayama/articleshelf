<script setup>
import { computed, reactive, watch } from 'vue'
import { X } from 'lucide-vue-next'

const props = defineProps({
  open: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'submit'])

const form = reactive(defaultForm())

const tagText = computed({
  get: () => form.tags.join(', '),
  set: (value) => {
    form.tags = value.split(',').map((tag) => tag.trim()).filter(Boolean)
  }
})

watch(
  () => props.open,
  (open) => {
    if (open) Object.assign(form, defaultForm())
  }
)

function defaultForm() {
  return {
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

function submit() {
  emit('submit', { ...form, readDate: form.readDate || null })
}
</script>

<template>
  <div v-if="open" class="modal-backdrop" @click.self="emit('close')">
    <form class="article-modal" @submit.prevent="submit">
      <div class="modal-header">
        <h2>記事を追加</h2>
        <button type="button" class="icon-button" title="閉じる" @click="emit('close')">
          <X :size="20" />
        </button>
      </div>

      <label>
        URL
        <input v-model="form.url" type="url" placeholder="https://example.com/article" required />
      </label>

      <label>
        タイトル
        <input v-model="form.title" placeholder="空欄ならOGPから補完します" />
      </label>

      <label>
        タグ
        <input v-model="tagText" placeholder="React, Next.js, 学習" />
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
        メモ
        <textarea v-model="form.notes" rows="5" placeholder="読んだポイントや次に試したいこと" />
      </label>

      <div class="modal-actions">
        <button type="button" class="ghost-button" @click="emit('close')">キャンセル</button>
        <button type="submit" class="primary-button">保存する</button>
      </div>
    </form>
  </div>
</template>
