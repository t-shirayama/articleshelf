<script setup lang="ts">
import { useI18n } from "vue-i18n";
import MotivationIllustration from "./MotivationIllustration.vue";
import type { MotivationCardData } from "../types";

defineProps<{
  card: MotivationCardData;
}>();

const { t } = useI18n();
</script>

<template>
  <section
    class="learning-boost-card"
    :aria-label="t('articles.motivationLabel')"
    :style="{
      '--boost-bg': card.background,
      '--boost-accent': card.accent,
      '--boost-ink': card.ink
    }"
  >
    <div>
      <strong>{{ card.title }}</strong>
      <span>{{ card.note }}</span>
    </div>

    <MotivationIllustration :card="card" />
  </section>
</template>
