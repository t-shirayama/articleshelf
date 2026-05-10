# 記事詳細・追加フォーム component 分割

## 状態

未対応

## 優先度

P3

## 目的

`ArticleDetail.vue` と `ArticleFormModal.vue` の template / form logic を分割し、表示専用、編集専用、メモ編集、作成フォームの責務を読みやすくする。

## 対象

- `frontend/src/features/articles/components/ArticleDetail.vue`
- `frontend/src/features/articles/components/ArticleFormModal.vue`
- `useArticleDetailForm`
- article create form state
- Markdown preview / notes editor

## 対応内容

- `ArticleDetailView`、`ArticleDetailEditForm`、`ArticleNotesEditor`、`ArticleNotesPreview` などの分割を検討する
- `ArticleDetailPage` が form state と save / delete handling を持つ構成を検討する
- `useArticleCreateForm` を追加し、追加モーダルのフォームロジックを切り出す
- 既存の validation、duplicate article 導線、readLater / readDate 仕様を維持する

## 完了条件

- Article detail の閲覧 / 編集 / notes preview の責務が分かれている
- Article create form の変換 / validation logic が composable または domain helper に整理されている
- 既存の UI 表示と操作フローが維持されている
- component / unit tests と design docs が必要に応じて更新されている

## 根拠

Frontend レビューで、`ArticleDetail.vue` は `useArticleDetailForm` で整理されている一方、template が長く、`ArticleFormModal.vue` もフォーム拡張時に composable 化すると見通しが良くなると指摘されたため。
