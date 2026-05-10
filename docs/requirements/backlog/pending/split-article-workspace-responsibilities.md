# ArticleWorkspace 責務分割

## 状態

未対応

## 優先度

P2

## 目的

`ArticleWorkspace.vue` に集中している navigation、dialog、view switching、search debounce、account 操作などの責務を分割し、画面全体の親コンポーネントを薄くする。

## 対象

- `frontend/src/features/articles/views/ArticleWorkspace.vue`
- mobile header / drawer / bottom navigation
- workspace dialogs
- search debounce
- article / tag / account 操作のイベント配線
- 関連 E2E / component tests

## 対応内容

- `MobileHeader`、`MobileDrawer`、`MobileBottomNavigation` などの layout component を抽出する
- `WorkspaceDialogs` を追加し、追加 / フィルタ / 削除確認 / snackbar / account settings dialog を集約する
- `WorkspaceMainView` または page component へ一覧 / カレンダー / タグ / 詳細の表示切り替えを分割する
- `useArticleSearchDebounce` などへ search debounce を切り出す
- `searchTimer` を component unmount 時に clear する
- 既存の見た目と操作フローを維持する

## 完了条件

- `ArticleWorkspace.vue` の template と script の責務が縮小されている
- navigation / dialogs / main view の責務が別 component または composable に分かれている
- logout や画面切り替え直後に search debounce が遅延実行されない
- 既存 E2E と必要な unit / component tests が新構成に追従している
- architecture / design / testing docs が必要な範囲で更新されている

## 根拠

Frontend レビューで、`ArticleWorkspace.vue` がサイドバー、モバイルナビ、view switching、search debounce、modal、filter、calendar、tag、detail、account 操作をまとめて扱っており、最大の肥大化ポイントだと指摘されたため。
