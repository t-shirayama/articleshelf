# Server driven article list query

## 状態

未対応

## 優先度

P1

## 目的

backend に入り始めている `page` / `size` / `sort` query を frontend store と接続し、10万件規模の記事データでも検索、絞り込み、並び順、ページ取得を説明できる構成へ寄せる。

## 対象

- `GET /api/articles`
- `frontend/src/features/articles/api/articlesApi.ts`
- `frontend/src/features/articles/stores/articles.ts`
- 記事一覧、検索、フィルタ、ソート、カレンダー、タグ管理とのデータ境界
- API / feature / frontend architecture / testing docs

## 対応内容

- backend 側の DB-backed pagination / sort contract は [DB backed article list pagination sort](db-backed-article-list-pagination-sort.md) を前提にし、frontend からはその契約に沿った query を送る。
- 既存 `/api/articles` の範囲で、検索、ステータス、タグ、お気に入り、並び順、ページ取得を frontend から query として渡す。
- デプロイ構成は変えず、Vue / Spring Boot / PostgreSQL / Docker Compose の現行構成内で完結させる。
- frontend store は全件取得を前提にした表示から、一覧用 query state と取得結果を持つ構成へ段階的に移す。
- カレンダーやタグ管理など全件集計に依存する画面は、必要な API 契約または当面の互換取得範囲を明確にする。
- 外部検索エンジン、別キャッシュ基盤、新しいインフラはこのタスクに含めない。

## 完了条件

- 記事一覧の検索、絞り込み、並び順、ページ取得が server-driven に動作する。
- frontend store が大量件数前提の一覧取得方針を持ち、全記事を常に正本として抱える構造から離れる。
- API docs に request query、レスポンス、互換方針、上限値が反映される。
- frontend architecture / testing docs に一覧 query state、回帰観点、カレンダー / タグ管理との関係が反映される。
- 既存 E2E の検索、フィルタ、ソート、詳細遷移、カレンダー、タグ管理が回帰しない。

## 根拠

backend には `page` / `size` / `sort` が入り始めているが、frontend store は基本的に全記事取得後の client-side filtering / sorting を使っている。小規模では問題ないが、大量データ時の設計説明では server-driven query との接続が必要になる。
加えて、backend 側でも page / size / sort を DB query に落とす必要があるため、frontend 接続とは別に backend persistence contract の改善を前提タスクとして扱う。
