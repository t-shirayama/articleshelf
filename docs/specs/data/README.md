# データモデル

公開に向けたユーザー登録・ログインでは、`User` と `RefreshToken` を追加し、`Article` と `Tag` はユーザーごとの所有データとして扱います。
`Article.url` と `Tag.name` の一意性は全体一意ではなくユーザー単位の一意性です。
詳細な認証・ユーザースコープ設計は [認証仕様](../auth/README.md) を参照してください。

## User

| カラム | 型 | 制約 | 説明 |
| --- | --- | --- | --- |
| id | UUID | PK | ユーザー ID |
| username | VARCHAR(32) | NOT NULL, UNIQUE | ログイン ID。小文字正規化 |
| email | VARCHAR | NULL | 移行互換用 legacy column。新規 API では返さない |
| password_hash | VARCHAR | NOT NULL | password hash |
| display_name | VARCHAR | NULL | 表示名 |
| role | VARCHAR | NOT NULL | `USER` または `ADMIN` |
| status | VARCHAR | NOT NULL | `ACTIVE`, `LOCKED`, `DELETED` |
| created_at | TIMESTAMP | NOT NULL | 登録日時 |
| updated_at | TIMESTAMP | NOT NULL | 更新日時 |
| last_login_at | TIMESTAMP | NULL | 最終ログイン日時 |
| token_valid_after | TIMESTAMP | NOT NULL | これより古い `iat` の access token を拒否する |

退会時は `status = DELETED` の論理削除とし、記事・タグは保持するが protected API から参照不可にします。

## RefreshToken

| カラム | 型 | 制約 | 説明 |
| --- | --- | --- | --- |
| id | UUID | PK | refresh token ID |
| user_id | UUID | FK, NOT NULL | users.id |
| token_hash | VARCHAR | NOT NULL, UNIQUE | refresh token の hash |
| family_id | UUID | NOT NULL | rotation 系列 |
| expires_at | TIMESTAMP | NOT NULL | 有効期限 |
| revoked_at | TIMESTAMP | NULL | 失効日時 |
| created_at | TIMESTAMP | NOT NULL | 発行日時 |
| replaced_by_token_id | UUID | NULL | rotation 後の token |
| user_agent | VARCHAR | NULL | 端末識別補助 |
| ip_address | VARCHAR | NULL | 監査補助 |

## Article

| API field | DB column | 型 | 制約 / 説明 |
| --- | --- | --- | --- |
| id | id | UUID | PK |
| userId | user_id | UUID | 所有ユーザー。protected API では current user にスコープする |
| url | url | string / VARCHAR | 必須、最大2048文字、ユーザー単位でユニーク |
| title | title | string / VARCHAR | 必須、最大255文字 |
| summary | summary | string / TEXT | 任意、API入力は最大5000文字 |
| thumbnailUrl | thumbnail_url | string / VARCHAR | 任意 |
| status | status | enum / VARCHAR | `UNREAD`, `READ` |
| readDate | read_date | date / DATE | 任意、API では `YYYY-MM-DD` |
| favorite | favorite | boolean / BOOLEAN | お気に入り |
| rating | rating | integer / INTEGER | `0` - `5`、未指定時は `0` |
| notes | notes | text / TEXT | 任意、API入力は最大20000文字 |
| createdAt | created_at | timestamp / TIMESTAMP | 登録日時 |
| updatedAt | updated_at | timestamp / TIMESTAMP | 更新日時 |

## Tag

| API field | DB column | 型 | 制約 / 説明 |
| --- | --- | --- | --- |
| id | id | UUID | PK |
| userId | user_id | UUID | 所有ユーザー |
| name | name | string / VARCHAR | ユーザー単位でユニーク |
| createdAt | created_at | timestamp / TIMESTAMP | 登録日時 |
| updatedAt | updated_at | timestamp / TIMESTAMP | 更新日時 |
| articleCount | - | integer | 一覧表示用の集計値。永続化カラムではない |

## ArticleTag

| API / model field | DB column | 型 | 制約 / 説明 |
| --- | --- | --- | --- |
| userId | user_id | UUID | 主キーの一部 |
| articleId | article_id | UUID | 主キーの一部 |
| tagId | tag_id | UUID | 主キーの一部 |

記事とタグは多対多で関連付けます。
`userId`, `articleId`, `tagId` を主キーにし、複合 FK で article / tag の user mismatch を拒否します。

## Migration Contract

- schema は Flyway の versioned migration で更新する
- `V1__baseline_schema.sql` は初回 schema と既存開発 DB の baseline 統合を兼ねるため、冪等な table / column 追加、既存制約の整理、既存データ補正を含む
- `V2__username_auth_and_account_controls.sql` は username login、legacy email nullable 化、`token_valid_after` 追加を担当する
- 共有環境や本番 DB に適用済みの migration は rewrite せず、以後の schema 変更は新しい migration として追加する
- migration と JPA Entity の差分は JPA validate と PostgreSQL 実体を使った integration test で確認する
