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
