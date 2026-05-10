<script setup lang="ts">
import { Eye, Pencil } from 'lucide-vue-next'
import MarkdownViewer from './MarkdownViewer.vue'
import type { ArticleDetailForm } from '../domain/articleForms'

defineProps<{
  form: ArticleDetailForm
  notesText: string
}>()

const previewOpen = defineModel<boolean>('previewOpen', { required: true })
</script>

<template>
  <section class="detail-section detail-edit-notes-section">
    <div class="detail-section-header detail-notes-header">
      <div class="detail-notes-heading-copy">
        <h3>{{ $t('common.notes') }}</h3>
        <span>{{ $t('detail.notesHelp') }}</span>
      </div>
      <VBtn
        class="detail-notes-preview-button"
        variant="outlined"
        color="primary"
        size="small"
        type="button"
        @click="previewOpen = !previewOpen"
      >
        <template #prepend>
          <Pencil v-if="previewOpen" :size="16" />
          <Eye v-else :size="16" />
        </template>
        {{ previewOpen ? $t('detail.notesEdit') : $t('detail.notesPreview') }}
      </VBtn>
    </div>
    <template v-if="previewOpen">
      <div class="detail-notes-preview-stack">
        <div class="detail-notes-preview">
          <MarkdownViewer v-if="form.notes.trim()" :source="form.notes" />
          <p v-else class="detail-body-copy detail-notes-copy is-empty">{{ notesText }}</p>
        </div>
        <div class="detail-notes-preview-details-spacer" aria-hidden="true" />
      </div>
    </template>
    <VTextarea
      v-else
      v-model="form.notes"
      class="detail-edit-notes-field"
      counter="20000"
      :aria-label="$t('common.notes')"
      rows="13"
      variant="outlined"
    />
  </section>
</template>
