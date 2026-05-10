# 記事一覧 pagination / query model 導入

## 状態

未対応

## 優先度

P2

## 目的

記事数増加時にも一覧取得の性能と UX を維持できるよう、backend pagination、sorting、query object、必要に応じた read model を導入する。

## 対象

- `GET /api/articles`
- Article search query
- article repository search
- frontend article list loading
- API specs / frontend specs

## 対応内容

- `page`、`size`、`sort` などの API query parameter を検討する
- response に `items`、`page`、`size`、`totalItems`、`totalPages` などを含めるか検討する
- 検索条件を query object として整理する
- 一覧用 read model / projection の導入要否を検討する
- frontend の一覧表示、検索、フィルタ、並び替えとの相互作用を整理する

## 完了条件

- 記事一覧 API の pagination / sorting 方針が仕様化されている
- backend repository / query object が pagination を扱える
- frontend の一覧 UX が pagination または incremental loading に対応している
- API / UI / testing docs が更新されている

## 根拠

Backend レビューで、`findArticles` が `List<ArticleResponse>` を返しており、将来の記事数増加を考えると pagination / sorting / query object を導入した方が性能と設計説明力が上がると指摘されたため。
