# Data Architecture

ArticleShelf の主要データは PostgreSQL に保存します。
カラム、制約、API 表現を含むデータモデルの正本は [データモデル仕様](../../specs/data/README.md) に集約します。

## 永続化方針

- 記事、タグ、ユーザー、refresh token は user scoped に扱う
- `Article.url` と `Tag.name` は全体一意ではなくユーザー単位の一意性として扱う
- `article_tags` は記事とタグの関連を保持し、`user_id` も主キーに含める
- article / tag の user mismatch は application 層の検証に加え、複合 FK で DB レベルでも拒否する
- 退会は `users.status = DELETED` の論理削除とし、記事・タグは保持するが protected API から参照不可にする
- JPA Entity は infrastructure 層の実装詳細として扱い、application / domain へ漏らさない

## スキーマ管理

- DB schema は Flyway migration で管理する
- Spring Data JPA は schema validation を行い、migration と Entity のずれを検知する
- PostgreSQL を正とし、DB 方言差が出やすい検索条件や制約変更では PostgreSQL 実体で確認する

## Migration 履歴の扱い

`V1__baseline_schema.sql` は初回リリース時点の新規 schema 作成だけでなく、既存の開発 DB をユーザー認証対応後の schema へ寄せる baseline 統合 migration として残している。
そのため `CREATE TABLE IF NOT EXISTS`、`ALTER TABLE ... ADD COLUMN IF NOT EXISTS`、既存 unique / FK 制約の整理、`article_tags.user_id` の補完、user mismatch の関連削除を同じ migration に含める。

`V2__username_auth_and_account_controls.sql` は、V1 後に追加した username login と account control 用の差分 migration として扱う。
本番または共有 DB に適用済みの migration は原則 rewrite せず、追加変更は新しい versioned migration で表現する。
