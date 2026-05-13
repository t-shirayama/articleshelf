<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { Download, Puzzle, X } from "lucide-vue-next";
import {
  extensionDownloadUrl,
  shouldLoadExtensionVersionFromGitHub,
} from "../../../shared/config/extensionDownload";

defineProps<{
  open: boolean;
}>();

const emit = defineEmits<{
  close: [];
}>();

const { t } = useI18n();

const extensionVersion = ref(
  normalizeVersionLabel(import.meta.env.VITE_EXTENSION_VERSION),
);
const releaseApiUrl = "https://api.github.com/repos/t-shirayama/articleshelf/releases/latest";

function normalizeVersionLabel(input: string | undefined): string {
  const value = input?.trim() ?? "";
  if (!value) return "latest";
  return value.startsWith("v") ? value.slice(1) : value;
}

async function loadExtensionVersionFromGitHub(): Promise<void> {
  try {
    const response = await fetch(releaseApiUrl, {
      headers: {
        Accept: "application/vnd.github+json",
      },
    });

    if (!response.ok) return;

    const payload = (await response.json()) as { tag_name?: string };
    if (payload.tag_name) {
      extensionVersion.value = normalizeVersionLabel(payload.tag_name);
    }
  } catch {
    // keep fallback value
  }
}

onMounted(() => {
  if (shouldLoadExtensionVersionFromGitHub) {
    void loadExtensionVersionFromGitHub();
  }
});

function handleDialogUpdate(open: boolean): void {
  if (!open) emit("close");
}
</script>

<template>
  <VDialog
    :model-value="open"
    max-width="560"
    content-class="help-dialog-overlay"
    @update:model-value="handleDialogUpdate"
  >
    <VCard class="help-dialog">
      <header class="article-modal-header help-dialog-header">
        <h2>
          <Puzzle :size="20" />
          <span>{{ t("help.title") }}</span>
        </h2>
        <div class="article-modal-header-actions">
          <VBtn
            icon
            variant="text"
            :aria-label="t('common.close')"
            @click="emit('close')"
          >
            <X :size="18" />
          </VBtn>
        </div>
      </header>

      <VCardText class="help-dialog-body">
        <section class="help-dialog-section">
          <div class="help-dialog-section-heading">
            <Puzzle :size="18" />
            <strong>{{ t("help.chromeExtensionTitle") }}</strong>
            <span class="help-dialog-chip">
              {{ t("help.extensionVersion", { version: extensionVersion }) }}
            </span>
          </div>
          <p>{{ t("help.chromeExtensionDescription") }}</p>
          <div class="help-dialog-actions">
            <VBtn
              color="primary"
              variant="flat"
              :href="extensionDownloadUrl"
              download
            >
              <template #prepend>
                <Download :size="18" />
              </template>
              {{ t("help.chromeExtensionDownload") }}
            </VBtn>
          </div>
          <p class="help-dialog-note">{{ t("help.chromeExtensionTarget") }}</p>
          <strong class="help-dialog-subtitle">{{ t("help.chromeExtensionStepsTitle") }}</strong>
          <ol class="help-dialog-steps">
            <li>{{ t("help.chromeExtensionStepOne") }}</li>
            <li>{{ t("help.chromeExtensionStepTwo") }}</li>
            <li>{{ t("help.chromeExtensionStepThree") }}</li>
            <li>{{ t("help.chromeExtensionStepFour") }}</li>
          </ol>
          <p>{{ t("help.chromeExtensionReinstall") }}</p>
        </section>
      </VCardText>
    </VCard>
  </VDialog>
</template>
