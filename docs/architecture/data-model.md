# Data Model

ReadStack の主要データは PostgreSQL に保存します。

## テーブル構成

- `articles`
  - id: UUID
  - user_id: UUID
  - url: VARCHAR
  - title: VARCHAR
  - summary: TEXT
  - thumbnail_url: VARCHAR
  - status: VARCHAR
  - read_date: DATE
  - favorite: BOOLEAN
  - rating: INTEGER
  - notes: TEXT
  - created_at: TIMESTAMP
  - updated_at: TIMESTAMP

- `tags`
  - id: UUID
  - user_id: UUID
  - name: VARCHAR
  - created_at: TIMESTAMP
  - updated_at: TIMESTAMP

- `article_tags`
  - article_id: UUID
  - tag_id: UUID

- `users`
  - id: UUID
  - email: VARCHAR
  - password_hash: VARCHAR
  - display_name: VARCHAR
  - role: VARCHAR
  - status: VARCHAR
  - created_at: TIMESTAMP
  - updated_at: TIMESTAMP
  - last_login_at: TIMESTAMP

- `refresh_tokens`
  - id: UUID
  - user_id: UUID
  - token_hash: VARCHAR
  - family_id: UUID
  - expires_at: TIMESTAMP
  - revoked_at: TIMESTAMP
  - created_at: TIMESTAMP
  - replaced_by_token_id: UUID
  - user_agent: VARCHAR
  - ip_address: VARCHAR

## 永続化方針

- 記事、タグ、ユーザー、refresh token は user scoped に扱う
- `article_tags` は記事とタグの関連を保持する
- article / tag の user mismatch は DB レベルでも拒否する
- JPA Entity は infrastructure 層の実装詳細として扱い、application / domain へ漏らさない
