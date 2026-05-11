import { ref, type Ref } from "vue";
import { ApiRequestError } from "../../../shared/api/client";
import { errorMessage } from "../../../shared/errors";
import type { useArticlesStore } from "../stores/articles";
import type { Article, ArticleInput } from "../types";
import type { WorkspaceViewMode } from "./useWorkspaceNavigation";

type ArticlesStore = ReturnType<typeof useArticlesStore>;
type Translate = (key: string) => string;

interface ArticleActionOptions {
  store: ArticlesStore;
  t: Translate;
  viewMode: Ref<WorkspaceViewMode>;
  detailHasUnsavedChanges: Ref<boolean>;
  articleFormError: Ref<string>;
  detailFormError: Ref<string>;
  duplicateArticleId: Ref<string>;
  modalOpen: Ref<boolean>;
  rotateMotivation: () => void;
  navigateToList: () => void;
  closeForDuplicateOpen: () => void;
  onCreateSuccess?: (article: Article) => void;
}

export function useArticleActions(options: ArticleActionOptions) {
  const isCreatingArticle = ref(false);
  const isSavingDetail = ref(false);
  const isDeletingArticle = ref(false);
  const deleteCandidate = ref<Article | null>(null);

  async function createArticle(article: ArticleInput): Promise<void> {
    if (isCreatingArticle.value) return;
    options.articleFormError.value = "";
    options.duplicateArticleId.value = "";
    isCreatingArticle.value = true;
    try {
      const created = await options.store.createArticle(article);
      options.rotateMotivation();
      options.modalOpen.value = false;
      options.viewMode.value = "list";
      options.onCreateSuccess?.(created);
    } catch (error: unknown) {
      options.articleFormError.value =
        errorMessage(error, options.t("articles.saveError"));
      options.duplicateArticleId.value =
        error instanceof ApiRequestError ? error.existingArticleId || "" : "";
    } finally {
      isCreatingArticle.value = false;
    }
  }

  async function saveArticle(article: ArticleInput): Promise<void> {
    if (isSavingDetail.value) return;
    options.detailFormError.value = "";
    isSavingDetail.value = true;
    try {
      await options.store.updateArticle(article);
    } catch (error: unknown) {
      options.detailFormError.value =
        errorMessage(error, options.t("articles.saveError"));
    } finally {
      isSavingDetail.value = false;
    }
  }

  async function deleteArticle(articleId: string): Promise<void> {
    if (isDeletingArticle.value) return;
    options.detailFormError.value = "";
    isDeletingArticle.value = true;
    try {
      await options.store.deleteArticle(articleId);
      options.detailHasUnsavedChanges.value = false;
      options.navigateToList();
    } catch (error: unknown) {
      options.detailFormError.value =
        errorMessage(error, options.t("articles.deleteError"));
    } finally {
      isDeletingArticle.value = false;
    }
  }

  function requestDeleteArticle(article: Article): void {
    deleteCandidate.value = article;
  }

  async function confirmListDelete(): Promise<void> {
    if (!deleteCandidate.value) return;
    const articleId = deleteCandidate.value.id;
    deleteCandidate.value = null;
    options.store.error = "";
    try {
      await options.store.deleteArticle(articleId);
    } catch (error: unknown) {
      options.store.error = errorMessage(error, options.t("articles.deleteError"));
    }
  }

  async function openArticle(article: Article): Promise<void> {
    options.detailFormError.value = "";
    options.store.error = "";
    try {
      await options.store.selectArticle(article);
      options.rotateMotivation();
      options.viewMode.value = "detail";
    } catch (error: unknown) {
      options.store.error = errorMessage(error, options.t("articles.fetchError"));
    }
  }

  async function toggleFavorite(article: Article): Promise<void> {
    await options.store.toggleFavorite(article);
  }

  async function openDuplicateArticle(articleId: string): Promise<void> {
    options.closeForDuplicateOpen();
    options.detailFormError.value = "";
    try {
      await options.store.selectArticleById(articleId);
      options.rotateMotivation();
      options.viewMode.value = "detail";
    } catch (error: unknown) {
      options.detailFormError.value = errorMessage(error, options.t("articles.fetchError"));
    }
  }

  return {
    deleteCandidate,
    isCreatingArticle,
    isSavingDetail,
    isDeletingArticle,
    createArticle,
    saveArticle,
    deleteArticle,
    requestDeleteArticle,
    confirmListDelete,
    openArticle,
    toggleFavorite,
    openDuplicateArticle,
  };
}
