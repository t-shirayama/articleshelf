import { ref, type Ref } from "vue";
import type { useArticlesStore } from "../stores/articles";

type ArticlesStore = ReturnType<typeof useArticlesStore>;
type Translate = (key: string) => string;

interface TagActionOptions {
  store: ArticlesStore;
  t: Translate;
  searchDraft: Ref<string>;
  navigateToList: () => void;
  requestNavigation: (action: () => void) => void;
}

export function useTagActions(options: TagActionOptions) {
  const isSavingTag = ref(false);

  async function renameTag(id: string, name: string): Promise<void> {
    if (isSavingTag.value) return;
    isSavingTag.value = true;
    options.store.error = "";
    try {
      await options.store.renameTag(id, name);
    } catch (error: unknown) {
      options.store.error = error instanceof Error ? error.message : options.t("tags.renameError");
    } finally {
      isSavingTag.value = false;
    }
  }

  async function addTag(name: string): Promise<void> {
    if (isSavingTag.value) return;
    isSavingTag.value = true;
    options.store.error = "";
    try {
      await options.store.createTag(name);
    } catch (error: unknown) {
      options.store.error = error instanceof Error ? error.message : options.t("tags.addError");
    } finally {
      isSavingTag.value = false;
    }
  }

  async function mergeTag(sourceId: string, targetId: string): Promise<void> {
    if (isSavingTag.value) return;
    isSavingTag.value = true;
    options.store.error = "";
    try {
      await options.store.mergeTag(sourceId, targetId);
    } catch (error: unknown) {
      options.store.error = error instanceof Error ? error.message : options.t("tags.mergeError");
    } finally {
      isSavingTag.value = false;
    }
  }

  async function deleteTag(id: string): Promise<void> {
    if (isSavingTag.value) return;
    isSavingTag.value = true;
    options.store.error = "";
    try {
      await options.store.deleteTag(id);
    } catch (error: unknown) {
      options.store.error = error instanceof Error ? error.message : options.t("tags.deleteError");
    } finally {
      isSavingTag.value = false;
    }
  }

  function openTagArticles(tagName: string): void {
    options.requestNavigation(() => {
      options.navigateToList();
      options.searchDraft.value = "";
      options.store.setSearch("");
      void options.store.setAllArticles();
      options.store.setTags([tagName]);
    });
  }

  return {
    isSavingTag,
    renameTag,
    addTag,
    mergeTag,
    deleteTag,
    openTagArticles,
  };
}
