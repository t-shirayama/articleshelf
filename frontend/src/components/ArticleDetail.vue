<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ArrowLeft, ExternalLink, Heart, Pencil, Save, Trash2, X } from 'lucide-vue-next'
import type { Article, ArticleInput, ArticleStatus, Tag } from '../types'

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
const tagOptions = computed(() => [...new Set(props.tags.map((tag) => tag.name).filter(Boolean))])
const statusOptions = [
  { label: '未読', value: 'UNREAD' },
  { label: '読了', value: 'READ' }
] satisfies Array<{ label: string, value: Exclude<ArticleStatus, 'ALL'> }>
const statusLabelMap = {
  UNREAD: '未読',
  READ: '読了'
} satisfies Record<Exclude<ArticleStatus, 'ALL'>, string>
const summaryText = computed(() => props.article?.summary?.trim() || '概要はまだありません')
const notesText = computed(() => props.article?.notes?.trim() || 'メモはまだありません')
const formattedReadDate = computed(() => {
  if (!props.article?.readDate) return '未設定'
  const parsed = new Date(props.article.readDate)
  if (Number.isNaN(parsed.getTime())) return props.article.readDate
  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  }).format(parsed)
})

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
          <VBtn icon :color="(isEditing ? form.favorite : article.favorite) ? 'primary' : undefined" title="お気に入り" @click="toggleFavorite">
            <Heart :size="19" :fill="(isEditing ? form.favorite : article.favorite) ? 'currentColor' : 'none'" />
          </VBtn>
          <template v-if="isEditing">
            <VBtn class="compact-button" variant="outlined" @click="cancelEditing">
              <template #prepend>
                <X :size="17" />
              </template>
              キャンセル
            </VBtn>
            <VBtn color="primary" type="submit" class="compact-button">
              <template #prepend>
                <Save :size="17" />
              </template>
              保存
            </VBtn>
          </template>
          <VBtn v-else color="primary" class="compact-button" @click="startEditing">
            <template #prepend>
              <Pencil :size="17" />
            </template>
            編集
          </VBtn>
        </div>
      </div>

      <div class="detail-header">
        <div>
          <p class="eyebrow">Article Detail</p>
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
              <div v-if="article.tags.length > 0" class="tag-list">
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

        <VCard class="detail-meta" variant="tonal">
          <VCardText class="detail-meta-content">
            <template v-if="isEditing">
              <div class="field-row stacked">
                <VSelect v-model="form.status" label="ステータス" :items="statusOptions" item-title="label" item-value="value" />
                <VTextField v-model="form.readDate" label="読了日" type="date" clearable />
              </div>

              <div class="rating-field detail-rating-field">
                <span>おすすめ度</span>
                <VRating v-model="form.rating" length="5" hover clearable density="comfortable" color="warning" />
              </div>
            </template>
            <template v-else>
              <div class="detail-meta-block">
                <span class="detail-meta-label">ステータス</span>
                <strong>{{ statusLabelMap[article.status] }}</strong>
              </div>
              <div class="detail-meta-block">
                <span class="detail-meta-label">読了日</span>
                <strong>{{ formattedReadDate }}</strong>
              </div>
              <div class="detail-meta-block">
                <span class="detail-meta-label">おすすめ度</span>
                <div class="rating-field detail-rating-field readonly-rating-field">
                  <VRating :model-value="article.rating" length="5" readonly density="comfortable" color="warning" />
                </div>
              </div>
            </template>

            <div class="detail-actions">
              <VBtn color="error" variant="outlined" @click="deleteDialogOpen = true">
                <template #prepend>
                  <Trash2 :size="17" />
                </template>
                削除
              </VBtn>
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
            <VBtn variant="text" @click="deleteDialogOpen = false">キャンセル</VBtn>
            <VBtn color="error" variant="flat" @click="confirmDelete">削除する</VBtn>
          </VCardActions>
        </VCard>
      </VDialog>
    </VForm>
  </main>
</template>
