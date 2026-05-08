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
  - user_id: UUID
  - article_id: UUID
  - tag_id: UUID

- `users`
  - id: UUID
  - username: VARCHAR
  - email: VARCHAR (移行互換用 nullable legacy column)
  - password_hash: VARCHAR
  - display_name: VARCHAR
  - role: VARCHAR
  - status: VARCHAR
  - created_at: TIMESTAMP
  - updated_at: TIMESTAMP
  - last_login_at: TIMESTAMP
  - token_valid_after: TIMESTAMP

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
- `users.username` はログイン ID として小文字正規化し、一意制約を持つ
- `users.email` は新規 API では返さない移行互換用 column として扱う
- `users.token_valid_after` より古い JWT access token は protected API で拒否する
- `article_tags` は記事とタグの関連を保持し、`user_id` も主キーに含める
- article / tag の user mismatch は DB レベルでも拒否する
- 退会は `users.status = DELETED` の論理削除とし、記事・タグは保持するが参照不可にする
- JPA Entity は infrastructure 層の実装詳細として扱い、application / domain へ漏らさない
