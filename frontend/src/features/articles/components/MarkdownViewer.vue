<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '../domain/renderMarkdown'

const props = defineProps<{
  source: string
}>()

const renderedHtml = computed(() => renderMarkdown(props.source))
</script>

<template>
  <div class="markdown-viewer" v-html="renderedHtml" />
</template>
