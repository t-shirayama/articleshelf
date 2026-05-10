# ArticleWorkspace container boundaries

## 状態

未対応

## 優先度

P1

## 目的

`ArticleWorkspace.vue` に残る認証、ルーティング同期、記事操作、タグ操作、モーダル、検索、カレンダー、モバイルナビ、アカウント設定の責務を分け、shell / route / feature container の境界を説明しやすくする。

## 対象

- `frontend/src/features/articles/views/ArticleWorkspace.vue`
- 記事一覧、記事詳細、カレンダー、タグ管理、アカウント設定の container / composable
- frontend architecture / designs / testing docs

## 対応内容

- Workspace 全体の shell、記事 route container、カレンダー route container、タグ管理 route container、設定 dialog container を分ける。
- account operation、mobile navigation、route sync、article action、tag action、modal state の責務を明確な composable または container に寄せる。
- 既存の unsaved changes 確認、detail return view、duplicate article open、logout state reset の挙動を維持する。
- UI 表示や文言の変更は最小限にし、責務境界の整理を主目的にする。

## 完了条件

- `ArticleWorkspace.vue` が巨大な feature 横断 component ではなく、shell と配線に近い薄い component になる。
- 主要 view ごとの container が route-level component から自然に呼べる境界を持つ。
- 認証操作、route 同期、記事操作、タグ操作、モーダル操作の所有者が architecture docs で説明される。
- 既存 unit / E2E で一覧、詳細、カレンダー、タグ管理、設定、ログアウト、未保存確認が回帰しない。

## 根拠

リファクタ後も `ArticleWorkspace.vue` は約750行あり、複数 feature と shell 責務を同時に持っている。composable 分割は進んでいるが、面接やレビューでは container 境界がまだ曖昧に見えやすいため、次の構造改善として管理する。
