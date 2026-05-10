import { computed, reactive, ref, watch, type Ref } from "vue";
import {
  articleToDetailForm,
  createEmptyArticleDetailForm,
  detailFormToArticleInput,
  favoriteToggleInput,
  hasArticleDetailFormChanges,
} from "../domain/articleForms";
import { readDateForStatus, todayString } from "../domain/articleStatus";
import type { Article, ArticleInput, ArticleStatus, Tag } from "../types";

type Translate = (key: string) => string;

export function useArticleDetailForm(
  article: Ref<Article | null>,
  tags: Ref<Tag[]>,
  t: Translate,
  onDirtyChange: (value: boolean) => void,
) {
  const form = reactive(createEmptyArticleDetailForm());
  const isEditing = ref(false);
  const articleDetailsOpen = ref(false);
  const submitted = ref(false);

  const detailMode = computed<"view" | "edit">({
    get: () => (isEditing.value ? "edit" : "view"),
    set: (value) => {
      if (value === "edit") {
        startEditing();
        return;
      }

      if (isEditing.value) {
        stopEditing();
      }
    },
  });

  const tagOptions = computed(() => [...new Set(tags.value.map((tag) => tag.name).filter(Boolean))]);
  const statusOptions = computed<Array<{ label: string; value: Exclude<ArticleStatus, "ALL"> }>>(() => [
    { label: t("articles.statusUnread"), value: "UNREAD" },
    { label: t("articles.statusRead"), value: "READ" },
  ]);
  const summaryText = computed(() => form.summary.trim() || t("detail.emptySummary"));
  const notesText = computed(() => form.notes.trim() || t("detail.emptyNotes"));
  const hasUnsavedChanges = computed(() => Boolean(
    article.value && hasArticleDetailFormChanges(form, article.value),
  ));
  const urlError = computed(() => (form.url.trim() ? "" : t("articleForm.validation.urlRequired")));
  const titleError = computed(() => (form.title.trim() ? "" : t("detail.titleRequired")));
  const readDateError = computed(() => (form.status === "READ" && !form.readDate ? t("detail.readDateRequired") : ""));
  const formValid = computed(() => !urlError.value && !titleError.value && !readDateError.value);

  watch(
    article,
    (currentArticle) => {
      Object.assign(form, currentArticle ? articleToDetailForm(currentArticle) : createEmptyArticleDetailForm());
      isEditing.value = false;
      articleDetailsOpen.value = false;
      submitted.value = false;
    },
    { immediate: true },
  );

  watch(
    () => form.status,
    (status, previousStatus) => {
      if (!isEditing.value || status === previousStatus) return;
      form.readDate = readDateForStatus(status, form.readDate, todayString());
    },
  );

  watch(hasUnsavedChanges, (value) => {
    onDirtyChange(value);
  }, { immediate: true });

  function createSubmitInput(saving: boolean): ArticleInput | null {
    submitted.value = true;
    if (!formValid.value || saving) return null;
    return detailFormToArticleInput(form);
  }

  function startEditing(): void {
    if (!article.value) return;
    isEditing.value = true;
    submitted.value = false;
  }

  function stopEditing(): void {
    isEditing.value = false;
    submitted.value = false;
  }

  function createFavoriteInput(): ArticleInput | null {
    if (!article.value) return null;
    if (isEditing.value || hasArticleDetailFormChanges(form, article.value)) {
      form.favorite = !form.favorite;
      return null;
    }

    return favoriteToggleInput(article.value);
  }

  return {
    form,
    deleteDialogOpen: ref(false),
    isEditing,
    articleDetailsOpen,
    submitted,
    detailMode,
    tagOptions,
    statusOptions,
    summaryText,
    notesText,
    urlError,
    titleError,
    readDateError,
    createSubmitInput,
    createFavoriteInput,
  };
}
