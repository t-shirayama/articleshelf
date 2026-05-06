<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ArrowLeft, ExternalLink, Heart, Save, Trash2 } from 'lucide-vue-next'
import StarRating from './StarRating.vue'
import TagEditor from './TagEditor.vue'
import type { Article, ArticleInput, ArticleStatus, Tag } from '../types'
import { formatDateTime } from '../utils/dateFormat'

interface ArticleDetailForm {
  id: string
  url: string
  title: string
  summary: string
  status: Exclude<ArticleStatus, 'ALL'>
  readDate: string | null
  favorite: boolean
  rating: number
  notes: string
  tags: string[]
}

const props = withDefaults(defineProps<{
  article?: Article | null
  tags?: Tag[]
}>(), {
  article: null,
  tags: () => []
})

const emit = defineEmits<{
  back: []
  save: [article: ArticleInput]
  delete: [articleId: string]
}>()

const form = reactive(emptyForm())
const deleteDialogOpen = ref(false)
const isEditing = ref(false)
const detailMode = computed<'view' | 'edit'>({
  get: () => (isEditing.value ? 'edit' : 'view'),
  set: (value) => {
    if (value === 'edit') {
      startEditing()
      return
    }

    if (isEditing.value) {
      cancelEditing()
    }
  }
})
const tagOptions = computed(() => [...new Set(props.tags.map((tag) => tag.name).filter(Boolean))])
const statusOptions = [
  { label: '未読', value: 'UNREAD' },
  { label: '読了', value: 'READ' }
] satisfies Array<{ label: string, value: Exclude<ArticleStatus, 'ALL'> }>
const summaryText = computed(() => props.article?.summary?.trim() || '概要はまだありません')
const notesText = computed(() => props.article?.notes?.trim() || 'メモはまだありません')

watch(
  () => props.article,
  (article) => {
    Object.assign(form, article ? toForm(article) : emptyForm())
    isEditing.value = false
  },
  { immediate: true }
)

function emptyForm(): ArticleDetailForm {
  return {
    id: '',
    url: '',
    title: '',
    summary: '',
    status: 'UNREAD',
    readDate: null,
    favorite: false,
    rating: 0,
    notes: '',
    tags: []
  }
}

function toForm(article: Article): ArticleDetailForm {
  return {
    id: article.id,
    url: article.url,
    title: article.title,
    summary: article.summary || '',
    status: article.status || 'UNREAD',
    readDate: article.readDate || null,
    favorite: article.favorite,
    rating: article.rating || 0,
    notes: article.notes || '',
    tags: article.tags?.map((tag) => tag.name) || []
  }
}

function submit(): void {
  emit('save', {
    ...form,
    tags: [...new Set(form.tags.map((tag) => tag.trim()).filter(Boolean))],
    readDate: form.readDate || null
  })
  isEditing.value = false
}

function startEditing(): void {
  if (!props.article) return
  Object.assign(form, toForm(props.article))
  isEditing.value = true
}

function cancelEditing(): void {
  Object.assign(form, props.article ? toForm(props.article) : emptyForm())
  isEditing.value = false
}

function toggleFavorite(): void {
  if (!props.article) return
  if (isEditing.value) {
    form.favorite = !form.favorite
    return
  }

  emit('save', {
    ...toForm(props.article),
    tags: props.article.tags.map((tag) => tag.name),
    readDate: props.article.readDate || null,
    favorite: !props.article.favorite
  })
}

function confirmDelete(): void {
  if (!props.article) return
  deleteDialogOpen.value = false
  emit('delete', props.article.id)
}
</script>

<template>
  <main class="detail-page" :class="{ 'is-editing': isEditing }">
    <div v-if="!article" class="empty-detail">
      <span>記事を選択すると詳細を編集できます。</span>
    </div>

    <VForm v-else class="detail-form" @submit.prevent="submit">
      <div class="detail-topbar">
        <VBtn class="action-button action-button-secondary compact-button detail-back-button" variant="outlined" @click="emit('back')">
          <template #prepend>
            <ArrowLeft :size="17" />
          </template>
          戻る
        </VBtn>

        <VBtnToggle v-model="detailMode" class="detail-mode-toggle" mandatory>
          <VBtn value="view">閲覧</VBtn>
          <VBtn value="edit">編集</VBtn>
        </VBtnToggle>

        <div class="detail-topbar-actions">
          <VBtn
            class="favorite-button detail-favorite-button"
            :class="{ 'is-active': isEditing ? form.favorite : article.favorite }"
            icon
            title="お気に入り"
            variant="text"
            @click="toggleFavorite"
          >
            <Heart :size="19" :fill="(isEditing ? form.favorite : article.favorite) ? 'currentColor' : 'none'" />
          </VBtn>
          <VBtn
            color="primary"
            variant="flat"
            type="submit"
            class="action-button action-button-primary compact-button"
            :disabled="!isEditing"
          >
            <template #prepend>
              <Save :size="17" />
            </template>
            保存
          </VBtn>
          <VBtn class="action-button action-button-danger-outline compact-button" color="error" variant="outlined" @click="deleteDialogOpen = true">
            <template #prepend>
              <Trash2 :size="17" />
            </template>
            削除
          </VBtn>
        </div>
      </div>

      <div class="detail-header">
        <div>
          <h2>{{ article.title }}</h2>
          <a :href="article.url" :title="article.url" target="_blank" rel="noreferrer">
            <ExternalLink :size="15" />
            <span class="detail-link-text">{{ article.url }}</span>
          </a>
        </div>
      </div>

      <div class="detail-layout">
        <section class="detail-main">
          <template v-if="isEditing">
            <VTextField v-model="form.title" label="タイトル" required />

            <VTextField v-model="form.url" label="URL" type="url" required />

            <VTextarea v-model="form.summary" label="概要" rows="5" auto-grow />

            <TagEditor
              v-model="form.tags"
              :options="tagOptions"
            />

            <VTextarea v-model="form.notes" label="メモ" rows="8" auto-grow />
          </template>
          <template v-else>
            <section class="detail-section">
              <div class="detail-section-header">
                <h3>概要</h3>
              </div>
              <p class="detail-body-copy" :class="{ 'is-empty': !article.summary }">
                {{ summaryText }}
              </p>
            </section>

            <section class="detail-section">
              <div class="detail-section-header">
                <h3>タグ</h3>
              </div>
              <div v-if="article.tags.length > 0" class="tag-list detail-tag-list">
                <VChip v-for="tag in article.tags" :key="tag.id || tag.name" size="small" color="secondary" variant="flat">
                  {{ tag.name }}
                </VChip>
              </div>
              <p v-else class="detail-body-copy is-empty">タグはまだありません</p>
            </section>

            <section class="detail-section">
              <div class="detail-section-header">
                <h3>メモ</h3>
              </div>
              <p class="detail-body-copy detail-notes-copy" :class="{ 'is-empty': !article.notes }">
                {{ notesText }}
              </p>
            </section>
          </template>
        </section>

        <VCard class="detail-meta" variant="flat">
          <VCardText class="detail-meta-content">
            <div class="detail-meta-block">
              <span class="detail-meta-label">ステータス</span>
              <VSelect
                v-model="form.status"
                class="readstack-select detail-meta-control"
                :items="statusOptions"
                item-title="label"
                item-value="value"
                density="comfortable"
                :disabled="!isEditing"
                hide-details
                variant="outlined"
              />
            </div>

            <div class="detail-meta-block">
              <span class="detail-meta-label">読了日</span>
              <VTextField
                v-model="form.readDate"
                class="readstack-date-field detail-meta-control"
                type="date"
                density="comfortable"
                :disabled="!isEditing"
                hide-details
                variant="outlined"
                :clearable="isEditing"
              />
            </div>

            <div class="detail-meta-block">
              <span class="detail-meta-label">登録日時</span>
              <VTextField
                class="readstack-date-field detail-meta-control"
                :model-value="formatDateTime(article.createdAt)"
                density="comfortable"
                disabled
                hide-details
                variant="outlined"
              />
            </div>

            <div class="detail-meta-block">
              <span class="detail-meta-label">更新日時</span>
              <VTextField
                class="readstack-date-field detail-meta-control"
                :model-value="formatDateTime(article.updatedAt)"
                density="comfortable"
                disabled
                hide-details
                variant="outlined"
              />
            </div>

            <div class="detail-meta-block">
              <span class="detail-meta-label">おすすめ度</span>
              <div class="rating-field detail-rating-field">
                <template v-if="isEditing">
                  <StarRating v-model="form.rating" :size="20" />
                </template>
                <template v-else>
                  <StarRating :model-value="article.rating" readonly :size="20" />
                </template>
              </div>
            </div>
          </VCardText>
        </VCard>
      </div>

      <VDialog v-model="deleteDialogOpen" max-width="420">
        <VCard class="delete-confirm-dialog" title="記事を削除しますか？">
          <VCardText>
            <p>
              「{{ article.title }}」を削除します。この操作は取り消せません。
            </p>
          </VCardText>
          <VCardActions>
            <VSpacer />
            <VBtn class="action-button action-button-secondary" variant="outlined" @click="deleteDialogOpen = false">キャンセル</VBtn>
            <VBtn class="action-button action-button-danger" color="error" variant="flat" @click="confirmDelete">削除する</VBtn>
          </VCardActions>
        </VCard>
      </VDialog>
    </VForm>
  </main>
</template>
