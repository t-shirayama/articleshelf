<script setup>
import { computed, reactive, ref, watch } from 'vue'

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
const submitted = ref(false)
const tagOptions = computed(() => [...new Set(props.tags.map((tag) => tag.name).filter(Boolean))])
const urlError = computed(() => (form.url.trim() ? '' : 'URLは必須です'))

const dialogOpen = computed({
  get: () => props.open,
  set: (value) => {
    if (!value) emit('close')
  }
})

watch(
  () => props.open,
  (open) => {
    if (open) {
      Object.assign(form, defaultForm())
      submitted.value = false
    }
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
  submitted.value = true
  if (urlError.value) return

  const readLater = form.readLater
  const tags = [...new Set(form.tags.map((tag) => tag.trim()).filter(Boolean))]
  emit('submit', {
    url: form.url.trim(),
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
          <VTextField
            v-model="form.url"
            label="URL"
            type="url"
            placeholder="https://example.com/article"
            required
            :error-messages="submitted && urlError ? [urlError] : []"
          />

          <div class="title-input-group">
            <VTextField
              v-model="form.title"
              label="タイトル（任意）"
              hide-details
              placeholder="例: Vue 3 の状態管理を学ぶ"
            />
            <p>未入力の場合はURLから取得した記事タイトルを自動設定します</p>
          </div>

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

          <div class="field-row modal-status-row">
            <label class="read-later-check">
              <input v-model="form.readLater" type="checkbox">
              <span class="read-later-box" aria-hidden="true" />
              <span class="read-later-copy">
                <strong>あとで読む</strong>
                <small>チェック中は未読として保存します</small>
              </span>
            </label>
            <VTextField
              v-model="form.readDate"
              class="read-date-field"
              label="読了日"
              type="date"
              :disabled="form.readLater"
              clearable
            />
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
