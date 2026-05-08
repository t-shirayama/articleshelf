import { ref } from "vue";
import type { useArticlesStore } from "../stores/articles";
import type { Article, ArticleStatus } from "../types";

type ArticlesStore = ReturnType<typeof useArticlesStore>;
type PersistedArticleStatus = Exclude<ArticleStatus, "ALL">;
type Translate = (key: string) => string;

interface StatusUndoState {
  articleId: string;
  status: PersistedArticleStatus;
  readDate: string | null;
}

export function useStatusUndo(store: ArticlesStore, t: Translate) {
  const statusUndo = ref<StatusUndoState | null>(null);
  const statusSnackbarOpen = ref(false);
  const statusSnackbarMessage = ref("");

  async function toggleArticleStatus(article: Article): Promise<void> {
    const previousStatus = article.status;
    const previousReadDate = article.readDate || null;
    const nextStatus: PersistedArticleStatus =
      article.status === "READ" ? "UNREAD" : "READ";
    const nextReadDate = nextStatus === "READ" ? todayString() : null;
    const updated = await store.updateArticleStatus(
      article,
      nextStatus,
      nextReadDate,
    );
    if (!updated) return;

    statusUndo.value = {
      articleId: article.id,
      status: previousStatus,
      readDate: previousReadDate,
    };
    statusSnackbarMessage.value =
      nextStatus === "READ" ? t("articles.statusReadDone") : t("articles.statusUnreadDone");
    statusSnackbarOpen.value = true;
  }

  async function undoArticleStatus(): Promise<void> {
    if (!statusUndo.value) return;
    const undo = statusUndo.value;
    const article = store.articles.find((item) => item.id === undo.articleId);
    if (!article) return;

    statusSnackbarOpen.value = false;
    statusUndo.value = null;
    await store.updateArticleStatus(article, undo.status, undo.readDate);
  }

  function todayString(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, "0");
    const day = String(today.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  return {
    statusSnackbarOpen,
    statusSnackbarMessage,
    toggleArticleStatus,
    undoArticleStatus,
  };
}
