<script setup>
import { computed, reactive, watch } from 'vue'

const props = defineProps({
  open: {
    type: Boolean,
    default: false
  },
  tags: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'submit'])

const form = reactive(defaultForm())
const tagOptions = computed(() => [...new Set(props.tags.map((tag) => tag.name).filter(Boolean))])

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
    readLater: true,
    readDate: null,
    favorite: false,
    notes: '',
    tags: []
  }
}

function submit() {
  const readLater = form.readLater
  const tags = [...new Set(form.tags.map((tag) => tag.trim()).filter(Boolean))]
  emit('submit', {
    url: form.url,
    title: form.title,
    summary: form.summary,
    status: readLater ? 'UNREAD' : 'READ',
    readDate: readLater ? null : form.readDate || null,
    favorite: form.favorite,
    notes: form.notes,
    tags
  })
}
</script>

<template>
  <VDialog v-model="dialogOpen" max-width="560">
    <VCard class="article-modal" title="記事を追加">
      <VForm @submit.prevent="submit">
        <VCardText>
          <VTextField v-model="form.url" label="URL" type="url" placeholder="https://example.com/article" required />

          <VTextField v-model="form.title" label="タイトル" placeholder="空欄ならOGPから補完します" />

          <VCombobox
            v-model="form.tags"
            label="タグ"
            :items="tagOptions"
            multiple
            chips
            closable-chips
            clearable
            placeholder="選択または入力"
          />

          <div class="field-row">
            <VCheckbox
              v-model="form.readLater"
              label="あとで読む"
              color="primary"
              hide-details
            />
            <VTextField v-model="form.readDate" label="読了日" type="date" :disabled="form.readLater" clearable />
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
