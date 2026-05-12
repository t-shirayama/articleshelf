import { ref } from "vue";
import type { ArticleFormSeed } from "../types";

export function useArticleModalState() {
  const modalOpen = ref(false);
  const articleFormError = ref("");
  const duplicateArticleId = ref("");
  const articleFormSeed = ref<ArticleFormSeed | null>(null);

  function openArticleModal(seed: ArticleFormSeed | null = null): void {
    articleFormError.value = "";
    duplicateArticleId.value = "";
    articleFormSeed.value = seed;
    modalOpen.value = true;
  }

  function closeArticleModal(isCreatingArticle: boolean): void {
    if (isCreatingArticle) return;
    articleFormError.value = "";
    duplicateArticleId.value = "";
    articleFormSeed.value = null;
    modalOpen.value = false;
  }

  function closeForDuplicateOpen(): void {
    articleFormError.value = "";
    duplicateArticleId.value = "";
    articleFormSeed.value = null;
    modalOpen.value = false;
  }

  return {
    modalOpen,
    articleFormError,
    duplicateArticleId,
    articleFormSeed,
    openArticleModal,
    closeArticleModal,
    closeForDuplicateOpen,
  };
}
