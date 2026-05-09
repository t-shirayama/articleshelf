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

- id: UUID
- userId: UUID
- url: string (必須、最大2048文字、ユーザー単位でユニーク)
- title: string (必須、最大255文字)
- summary: string (任意、API入力は最大5000文字)
- thumbnailUrl: string (任意)
- status: enum(`UNREAD`, `READ`)
- readDate: date (任意)
- favorite: boolean
- rating: integer (`0` - `5`, 未指定時は `0`)
- notes: text (任意、API入力は最大20000文字)
- createdAt: timestamp
- updatedAt: timestamp

## Tag

- id: UUID
- userId: UUID
- name: string
- createdAt: timestamp
- updatedAt: timestamp
- articleCount: integer (一覧表示用の集計値)

## ArticleTag

- userId: UUID
- articleId: UUID
- tagId: UUID
- 記事とタグは多対多で関連付ける
- `userId`, `articleId`, `tagId` を主キーにし、複合 FK で article / tag の user mismatch を拒否する
