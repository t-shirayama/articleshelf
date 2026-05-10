# DB backed article list pagination sort

## 状態

未対応

## 優先度

P1

## 目的

記事一覧 API の `page` / `size` / `sort` を application layer の after-fetch slice ではなく DB query に落とし込み、大量データ時も LIMIT / OFFSET と安定した sort contract を説明できる構成にする。

## 対象

- `GET /api/articles`
- `ArticleListQuery`
- `SearchArticlesQuery`
- `ArticleRepository` / JPA repository
- PostgreSQL persistence integration test
- API / data / backend architecture / testing docs

## 対応内容

- `ArticleListQuery.slice(...)` による取得後 `subList` をやめ、repository 境界で page、size、sort を受け取れる query model にする。
- Spring Data JPA の `Pageable` / `Sort` または同等の明示的 query 実装で、PostgreSQL 側の LIMIT / OFFSET / ORDER BY を使う。
- `sort` の許可値、既定値、未知値の扱い、同一値時の tie-breaker を API 契約として固定する。
- `createdAt desc` 固定の query から、API の `sort` パラメータと同じ順序へ切り替える。
- paging / sort / tag join / search / favorite / nullable filter の組み合わせを PostgreSQL 実体で検証する。

## 完了条件

- 記事一覧の page / size が DB 側で適用され、application layer で全件取得後に slice しない。
- `sort` パラメータが DB query の ORDER BY として反映される。
- 既定 sort と許可 sort 値が API docs に明記される。
- PostgreSQL integration test で LIMIT / OFFSET、sort、filter の組み合わせが確認される。
- `server-driven-article-list-query` 実装時の backend 前提として参照できる。

## 根拠

現状は `ArticleListQuery` が `page` / `size` / `sort` を持つ一方、`SearchArticlesQuery` が repository から取得した `List` に `query.slice(...)` を適用している。JPA query も `order by article.createdAt desc` 固定で、`sort` が実質的に DB contract になっていないため、ポートフォリオや面接で大量データ時の設計として突っ込まれやすい。
