<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Tag, X } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  modelValue: string[]
  options?: string[]
  allowCreate?: boolean
}>(), {
  options: () => [],
  allowCreate: true
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const { t } = useI18n()
const selectedExistingTag = ref('')
const existingTagSearch = ref('')
const newTag = ref('')

const selectedTags = computed(() => normalizeTags(props.modelValue))
const optionTags = computed(() => normalizeTags(props.options))
const availableOptions = computed(() => optionTags.value.filter((tag) => !selectedTags.value.includes(tag)))
const normalizedNewTag = computed(() => normalizeTag(newTag.value))
const canAddNewTag = computed(() => Boolean(normalizedNewTag.value) && !selectedTags.value.includes(normalizedNewTag.value))

async function addExistingTag(tag: string): Promise<void> {
  const normalized = normalizeTag(tag)
  if (!normalized || selectedTags.value.includes(normalized)) return

  emit('update:modelValue', [...selectedTags.value, normalized])
  await nextTick()
  selectedExistingTag.value = ''
  existingTagSearch.value = ''
}

function addNewTag(): void {
  if (!canAddNewTag.value) return

  emit('update:modelValue', [...selectedTags.value, normalizedNewTag.value])
  newTag.value = ''
}

function removeTag(tag: string): void {
  emit('update:modelValue', selectedTags.value.filter((selectedTag) => selectedTag !== tag))
}

function normalizeTag(tag: string): string {
  return tag.trim()
}

function normalizeTags(tags: string[]): string[] {
  return [...new Set(tags.map(normalizeTag).filter(Boolean))]
}
</script>

<template>
  <div class="tag-editor">
    <div class="tag-editor-selected">
      <span class="tag-editor-label">{{ t('articleForm.selectedTags') }}</span>
      <div v-if="selectedTags.length > 0" class="tag-editor-chip-list">
        <VChip
          v-for="tag in selectedTags"
          :key="tag"
          class="tag-editor-chip"
          closable
          color="secondary"
          size="small"
          variant="flat"
          @click:close="removeTag(tag)"
        >
          <template #prepend>
            <Tag :size="13" />
          </template>
          {{ tag }}
          <template #close>
            <X :size="14" />
          </template>
        </VChip>
      </div>
      <p v-else class="tag-editor-empty">{{ t('articleForm.noSelectedTags') }}</p>
    </div>

    <div class="tag-editor-controls">
      <VAutocomplete
        v-model="selectedExistingTag"
        v-model:search="existingTagSearch"
        class="readstack-select tag-editor-existing"
        :disabled="availableOptions.length === 0"
        :items="availableOptions"
        clearable
        density="comfortable"
        hide-details
        :label="t('articleForm.existingTag')"
        variant="outlined"
        @update:model-value="addExistingTag(String($event || ''))"
      />

      <div v-if="props.allowCreate" class="tag-editor-new">
        <VTextField
          v-model="newTag"
          class="tag-editor-new-input"
          density="comfortable"
          hide-details
          :label="t('articleForm.newTag')"
          :placeholder="t('articleForm.newTagPlaceholder')"
          variant="outlined"
          @keydown.enter.prevent="addNewTag"
        />
        <VBtn
          class="action-button action-button-secondary tag-editor-add-button"
          :disabled="!canAddNewTag"
          type="button"
          variant="outlined"
          @click="addNewTag"
        >
          <template #prepend>
            <Plus :size="16" />
          </template>
          {{ t('common.add') }}
        </VBtn>
      </div>
    </div>
  </div>
</template>
