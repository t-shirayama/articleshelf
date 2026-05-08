<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { MotivationCardData } from '../types'

defineProps<{
  card: MotivationCardData
}>()

const { t } = useI18n()
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

    <svg class="boost-illustration" viewBox="0 0 132 112" role="img" :aria-label="t('articles.motivationIllustrationAlt', { title: card.title })">
      <rect x="18" y="80" width="74" height="13" rx="4" fill="var(--boost-bg)" />
      <rect x="28" y="92" width="74" height="12" rx="4" fill="var(--boost-accent)" opacity="0.34" />

      <g v-if="card.illustration === 'plant'">
        <path d="M46 68h38l-5 35H51l-5-35Z" fill="var(--boost-accent)" opacity="0.8" />
        <path d="M65 70c-1-17 1-32 4-45" stroke="var(--boost-ink)" stroke-width="4" stroke-linecap="round" />
        <path d="M68 42c11-14 23-17 34-15-4 13-15 21-34 15Z" fill="var(--boost-accent)" />
        <path d="M65 54c-15-11-20-23-18-36 14 4 23 16 18 36Z" fill="var(--boost-accent)" opacity="0.72" />
      </g>

      <g v-else-if="card.illustration === 'books'">
        <rect x="34" y="42" width="58" height="13" rx="3" fill="var(--boost-accent)" />
        <rect x="26" y="56" width="72" height="13" rx="3" fill="var(--boost-bg)" stroke="var(--boost-ink)" stroke-width="2" />
        <rect x="38" y="70" width="56" height="13" rx="3" fill="var(--boost-accent)" opacity="0.72" />
        <path d="M47 42v41" stroke="var(--boost-ink)" stroke-width="2" opacity="0.45" />
      </g>

      <g v-else-if="card.illustration === 'spark'">
        <path d="M67 25l8 23 23 8-23 8-8 23-8-23-23-8 23-8 8-23Z" fill="var(--boost-accent)" />
        <circle cx="35" cy="32" r="5" fill="var(--boost-ink)" opacity="0.7" />
        <circle cx="98" cy="77" r="4" fill="var(--boost-ink)" opacity="0.5" />
      </g>

      <g v-else-if="card.illustration === 'mountain'">
        <path d="M24 84l28-42 17 24 11-16 30 34H24Z" fill="var(--boost-accent)" />
        <path d="M52 42l8 12-12 5 4-17Z" fill="#fff" opacity="0.85" />
        <circle cx="92" cy="28" r="8" fill="var(--boost-ink)" opacity="0.55" />
      </g>

      <g v-else-if="card.illustration === 'lamp'">
        <path d="M52 50h30l-5 22H57l-5-22Z" fill="var(--boost-accent)" />
        <path d="M67 72v18" stroke="var(--boost-ink)" stroke-width="4" stroke-linecap="round" />
        <path d="M55 92h24" stroke="var(--boost-ink)" stroke-width="4" stroke-linecap="round" />
        <path d="M47 39c9-13 29-13 39 0" stroke="var(--boost-accent)" stroke-width="5" stroke-linecap="round" opacity="0.5" />
      </g>

      <g v-else-if="card.illustration === 'rocket'">
        <path d="M68 24c17 11 19 30 7 49l-16-16c5-17 2-25 9-33Z" fill="var(--boost-accent)" />
        <circle cx="70" cy="42" r="5" fill="#fff" opacity="0.88" />
        <path d="M57 60l-15 8 7-16" fill="var(--boost-ink)" opacity="0.75" />
        <path d="M72 75l-8 15 16-7" fill="var(--boost-ink)" opacity="0.75" />
      </g>

      <g v-else-if="card.illustration === 'compass'">
        <circle cx="66" cy="58" r="31" fill="var(--boost-bg)" stroke="var(--boost-ink)" stroke-width="3" />
        <path d="M77 41L69 66l-24 9 8-25 24-9Z" fill="var(--boost-accent)" />
        <circle cx="66" cy="58" r="4" fill="var(--boost-ink)" />
      </g>

      <g v-else-if="card.illustration === 'notebook'">
        <rect x="39" y="30" width="54" height="64" rx="6" fill="var(--boost-bg)" stroke="var(--boost-ink)" stroke-width="3" />
        <path d="M52 47h28M52 60h22M52 73h30" stroke="var(--boost-accent)" stroke-width="4" stroke-linecap="round" />
        <path d="M39 42h-7M39 56h-7M39 70h-7" stroke="var(--boost-ink)" stroke-width="3" stroke-linecap="round" />
      </g>

      <g v-else-if="card.illustration === 'seed'">
        <ellipse cx="66" cy="72" rx="22" ry="15" fill="var(--boost-accent)" opacity="0.78" />
        <path d="M66 70c0-22 9-34 26-42" stroke="var(--boost-ink)" stroke-width="4" stroke-linecap="round" />
        <path d="M66 52c-11-10-20-13-31-9 7 12 18 16 31 9Z" fill="var(--boost-accent)" />
      </g>

      <g v-else-if="card.illustration === 'coffee'">
        <path d="M45 51h42v24c0 10-8 18-18 18h-6c-10 0-18-8-18-18V51Z" fill="var(--boost-accent)" />
        <path d="M87 58h8c7 0 7 13 0 13h-8" stroke="var(--boost-ink)" stroke-width="4" fill="none" />
        <path d="M54 35c-4-5 4-7 0-12M68 35c-4-5 4-7 0-12M82 35c-4-5 4-7 0-12" stroke="var(--boost-ink)" stroke-width="3" stroke-linecap="round" opacity="0.55" />
      </g>

      <g v-else-if="card.illustration === 'telescope'">
        <path d="M37 62l47-23 8 14-47 23-8-14Z" fill="var(--boost-accent)" />
        <circle cx="91" cy="46" r="9" fill="var(--boost-bg)" stroke="var(--boost-ink)" stroke-width="3" />
        <path d="M60 67l-13 24M66 64l12 27" stroke="var(--boost-ink)" stroke-width="4" stroke-linecap="round" />
        <circle cx="35" cy="32" r="4" fill="var(--boost-ink)" opacity="0.5" />
      </g>

      <g v-else-if="card.illustration === 'puzzle'">
        <path d="M36 43h24c-2-9 10-13 13-5 2 6-3 9-8 9v18h18c0-6 4-10 9-8 9 3 5 15-5 13v24H36V70c9 2 13-10 5-13-6-2-10 2-10 8V43h5Z" fill="var(--boost-accent)" />
        <path d="M36 43h24M65 47v18M65 70v24" stroke="var(--boost-ink)" stroke-width="3" opacity="0.45" />
      </g>

      <g v-else-if="card.illustration === 'bridge'">
        <path d="M26 82h80" stroke="var(--boost-ink)" stroke-width="5" stroke-linecap="round" />
        <path d="M34 80c7-28 57-28 64 0" stroke="var(--boost-accent)" stroke-width="8" stroke-linecap="round" />
        <path d="M43 74V53M58 74V43M74 74V43M89 74V53" stroke="var(--boost-ink)" stroke-width="3" stroke-linecap="round" opacity="0.55" />
      </g>

      <g v-else-if="card.illustration === 'archive'">
        <rect x="33" y="38" width="66" height="16" rx="4" fill="var(--boost-accent)" />
        <path d="M39 54h54l-5 39H44l-5-39Z" fill="var(--boost-bg)" stroke="var(--boost-ink)" stroke-width="3" />
        <path d="M57 66h18" stroke="var(--boost-accent)" stroke-width="4" stroke-linecap="round" />
      </g>

      <g v-else-if="card.illustration === 'ladder'">
        <path d="M47 91l16-62M85 91L69 29" stroke="var(--boost-ink)" stroke-width="5" stroke-linecap="round" />
        <path d="M56 55h20M52 70h28M61 40h10" stroke="var(--boost-accent)" stroke-width="5" stroke-linecap="round" />
        <circle cx="90" cy="31" r="6" fill="var(--boost-accent)" opacity="0.7" />
      </g>

      <g v-else-if="card.illustration === 'key'">
        <circle cx="49" cy="58" r="15" fill="var(--boost-bg)" stroke="var(--boost-ink)" stroke-width="4" />
        <path d="M63 58h36M84 58v12M94 58v8" stroke="var(--boost-accent)" stroke-width="6" stroke-linecap="round" />
        <circle cx="49" cy="58" r="5" fill="var(--boost-accent)" />
      </g>

      <g v-else-if="card.illustration === 'map'">
        <path d="M29 36l24-8 26 10 24-8v58l-24 8-26-10-24 8V36Z" fill="var(--boost-bg)" stroke="var(--boost-ink)" stroke-width="3" />
        <path d="M53 28v58M79 38v58" stroke="var(--boost-ink)" stroke-width="2" opacity="0.35" />
        <path d="M42 58c11-10 29 10 48-8" stroke="var(--boost-accent)" stroke-width="5" stroke-linecap="round" fill="none" />
      </g>

      <g v-else-if="card.illustration === 'clock'">
        <circle cx="66" cy="62" r="31" fill="var(--boost-bg)" stroke="var(--boost-ink)" stroke-width="4" />
        <path d="M66 62V42M66 62l16 10" stroke="var(--boost-accent)" stroke-width="5" stroke-linecap="round" />
        <path d="M52 26l-12 10M80 26l12 10" stroke="var(--boost-ink)" stroke-width="4" stroke-linecap="round" opacity="0.55" />
      </g>

      <g v-else-if="card.illustration === 'cloud'">
        <path d="M43 76c-12 0-18-8-18-17 0-10 8-17 18-17 5-13 24-18 36-7 12 0 22 9 22 21 0 11-8 20-22 20H43Z" fill="var(--boost-accent)" opacity="0.82" />
        <path d="M48 86h36M56 96h20" stroke="var(--boost-ink)" stroke-width="4" stroke-linecap="round" opacity="0.45" />
      </g>

      <g v-else-if="card.illustration === 'pencil'">
        <path d="M40 79l10-24 33-33 17 17-33 33-27 7Z" fill="var(--boost-accent)" />
        <path d="M83 22l17 17M50 55l17 17" stroke="var(--boost-ink)" stroke-width="4" stroke-linecap="round" />
        <path d="M40 79l9-3-6-6-3 9Z" fill="var(--boost-ink)" />
      </g>

      <g v-else-if="card.illustration === 'flag'">
        <path d="M45 92V27" stroke="var(--boost-ink)" stroke-width="5" stroke-linecap="round" />
        <path d="M48 30h45l-9 16 9 16H48V30Z" fill="var(--boost-accent)" />
        <path d="M38 92h25" stroke="var(--boost-ink)" stroke-width="5" stroke-linecap="round" />
      </g>

      <g v-else-if="card.illustration === 'binoculars'">
        <path d="M34 52h22l6 34H37c-10 0-15-10-10-19l7-15Z" fill="var(--boost-accent)" />
        <path d="M76 52h22l7 15c5 9 0 19-10 19H70l6-34Z" fill="var(--boost-accent)" opacity="0.78" />
        <path d="M56 52c5-10 15-10 20 0" stroke="var(--boost-ink)" stroke-width="4" stroke-linecap="round" />
        <circle cx="45" cy="75" r="7" fill="var(--boost-bg)" />
        <circle cx="87" cy="75" r="7" fill="var(--boost-bg)" />
      </g>

      <g v-else-if="card.illustration === 'tree'">
        <path d="M66 58v35" stroke="var(--boost-ink)" stroke-width="6" stroke-linecap="round" />
        <circle cx="54" cy="48" r="18" fill="var(--boost-accent)" opacity="0.78" />
        <circle cx="76" cy="46" r="21" fill="var(--boost-accent)" />
        <circle cx="65" cy="30" r="15" fill="var(--boost-bg)" stroke="var(--boost-accent)" stroke-width="4" />
      </g>

      <g v-else-if="card.illustration === 'toolbox'">
        <rect x="31" y="50" width="70" height="39" rx="6" fill="var(--boost-accent)" />
        <path d="M53 50v-9c0-5 4-8 9-8h8c5 0 9 3 9 8v9" stroke="var(--boost-ink)" stroke-width="4" fill="none" />
        <path d="M31 64h70M62 64v10h10V64" stroke="var(--boost-ink)" stroke-width="3" />
      </g>

      <g v-else-if="card.illustration === 'anchor'">
        <circle cx="66" cy="33" r="9" fill="var(--boost-bg)" stroke="var(--boost-ink)" stroke-width="4" />
        <path d="M66 42v44M45 60h42" stroke="var(--boost-accent)" stroke-width="6" stroke-linecap="round" />
        <path d="M38 72c4 18 17 25 28 25s24-7 28-25M38 72h13M94 72H81" stroke="var(--boost-ink)" stroke-width="4" stroke-linecap="round" fill="none" />
      </g>

      <g v-else-if="card.illustration === 'gem'">
        <path d="M43 35h46l16 21-39 42-39-42 16-21Z" fill="var(--boost-accent)" />
        <path d="M43 35l23 63 23-63M27 56h78M55 35L44 56M77 35l11 21" stroke="var(--boost-ink)" stroke-width="3" opacity="0.45" />
      </g>

      <g v-else-if="card.illustration === 'shield'">
        <path d="M66 26l34 13v24c0 21-14 32-34 41-20-9-34-20-34-41V39l34-13Z" fill="var(--boost-accent)" />
        <path d="M52 63l10 10 20-24" stroke="#fff" stroke-width="6" stroke-linecap="round" stroke-linejoin="round" />
      </g>

      <g v-else-if="card.illustration === 'calendar'">
        <rect x="34" y="34" width="64" height="58" rx="7" fill="var(--boost-bg)" stroke="var(--boost-ink)" stroke-width="4" />
        <path d="M34 51h64" stroke="var(--boost-accent)" stroke-width="6" />
        <path d="M50 27v14M82 27v14" stroke="var(--boost-ink)" stroke-width="5" stroke-linecap="round" />
        <circle cx="54" cy="67" r="4" fill="var(--boost-accent)" />
        <circle cx="68" cy="67" r="4" fill="var(--boost-accent)" />
        <circle cx="82" cy="67" r="4" fill="var(--boost-accent)" />
      </g>

      <g v-else-if="card.illustration === 'headphones'">
        <path d="M37 66c0-22 13-36 29-36s29 14 29 36" stroke="var(--boost-ink)" stroke-width="5" stroke-linecap="round" fill="none" />
        <rect x="29" y="62" width="18" height="28" rx="8" fill="var(--boost-accent)" />
        <rect x="85" y="62" width="18" height="28" rx="8" fill="var(--boost-accent)" />
        <path d="M54 88c9 7 18 7 27 0" stroke="var(--boost-ink)" stroke-width="4" stroke-linecap="round" opacity="0.45" />
      </g>

      <g v-else>
        <path d="M29 61l75-31-23 60-16-23-22 16-14-22Z" fill="var(--boost-accent)" />
        <path d="M104 30L65 67" stroke="var(--boost-ink)" stroke-width="4" stroke-linecap="round" opacity="0.45" />
      </g>
    </svg>
  </section>
</template>
