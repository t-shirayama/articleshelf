import { ref } from "vue";

export function useArticleModalState() {
  const modalOpen = ref(false);
  const articleFormError = ref("");
  const duplicateArticleId = ref("");

  function openArticleModal(): void {
    articleFormError.value = "";
    duplicateArticleId.value = "";
    modalOpen.value = true;
  }

  function closeArticleModal(isCreatingArticle: boolean): void {
    if (isCreatingArticle) return;
    articleFormError.value = "";
    duplicateArticleId.value = "";
    modalOpen.value = false;
  }

  function closeForDuplicateOpen(): void {
    articleFormError.value = "";
    duplicateArticleId.value = "";
    modalOpen.value = false;
  }

  return {
    modalOpen,
    articleFormError,
    duplicateArticleId,
    openArticleModal,
    closeArticleModal,
    closeForDuplicateOpen,
  };
}
