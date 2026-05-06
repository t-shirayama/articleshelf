<script setup>
import { computed, reactive, watch } from 'vue'

const props = defineProps({
  open: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'submit'])

const form = reactive(defaultForm())
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

const dialogOpen = computed({
  get: () => props.open,
  set: (value) => {
    if (!value) emit('close')
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
    readDate: null,
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
  <VDialog v-model="dialogOpen" max-width="560">
    <VCard class="article-modal" title="記事を追加">
      <VForm @submit.prevent="submit">
        <VCardText>
          <VTextField v-model="form.url" label="URL" type="url" placeholder="https://example.com/article" required />

          <VTextField v-model="form.title" label="タイトル" placeholder="空欄ならOGPから補完します" />

          <VTextField v-model="tagText" label="タグ" placeholder="React, Next.js, 学習" />

          <div class="field-row">
            <VSelect v-model="form.status" label="ステータス" :items="statusOptions" item-title="label" item-value="value" />
            <VTextField v-model="form.readDate" label="読了日" type="date" clearable />
          </div>

          <VTextarea
            v-model="form.notes"
            label="メモ"
            rows="5"
            auto-grow
            placeholder="読んだポイントや次に試したいこと"
          />
        </VCardText>

        <VCardActions>
          <VSpacer />
          <VBtn @click="emit('close')">キャンセル</VBtn>
          <VBtn color="primary" type="submit">保存する</VBtn>
        </VCardActions>
      </VForm>
    </VCard>
  </VDialog>
</template>
