# Article store state ownership 整理

## 状態

未対応

## 優先度

P1

## 目的

`useArticlesStore` に集中している server data、selected article、tags、filters、CRUD mutation、optimistic update、rollback の責務を整理し、状態の canonical source を明確にする。

## 対象

- `frontend/src/features/articles/stores/articles.ts`
- article query / mutation state
- article filters / sort
- tag state
- optimistic update / rollback
- `articles` / `allArticles` の二重管理

## 対応内容

- `articles` と `allArticles` のどちらを canonical source とするか決める
- 不要な二重管理を削除する、または computed / filtered result として責務を明確化する
- 必要に応じて `articleQueryStore`、`articleMutationStore`、`articleFilterStore`、`tagStore` へ分割する
- optimistic update と rollback の責務を整理する
- 既存の検索、フィルタ、タグ管理、詳細選択の挙動を維持する

## 完了条件

- article list state の canonical source が明確になっている
- `articles` / `allArticles` の乖離リスクが解消または docs に明記されている
- query、mutation、filter、tag の責務が store または composable で分かれている
- 既存の optimistic update / rollback tests が維持または更新されている
- frontend architecture docs が更新されている

## 根拠

Frontend レビューで、`useArticlesStore` が server data cache、selected article、tags、loading / error、filters、sort、CRUD mutation、optimistic update、rollback をまとめて持っており、`articles` と `allArticles` の二重管理が将来の乖離リスクになると指摘されたため。
