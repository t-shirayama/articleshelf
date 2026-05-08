# データモデル

公開に向けたユーザー登録・ログインでは、`User` と `RefreshToken` を追加し、`Article` と `Tag` はユーザーごとの所有データとして扱います。
`Article.url` と `Tag.name` の一意性は全体一意ではなくユーザー単位の一意性です。
詳細な認証・ユーザースコープ設計は `authentication.md` を参照してください。

## User

- id: UUID
- username: string (必須、3〜32文字、ユニーク、小文字正規化)
- email: string (任意、移行互換用 legacy column。新規 API では返さない)
- passwordHash: string
- displayName: string
- role: enum(`USER`, `ADMIN`)
- status: enum(`ACTIVE`, `LOCKED`, `DELETED`)
- tokenValidAfter: timestamp
- createdAt: timestamp
- updatedAt: timestamp
- lastLoginAt: timestamp (任意)

退会時は `status = DELETED` の論理削除とし、記事・タグは保持するが protected API から参照不可にします。

## RefreshToken

- id: UUID
- userId: UUID
- tokenHash: string (ユニーク)
- familyId: UUID
- expiresAt: timestamp
- revokedAt: timestamp (任意)
- createdAt: timestamp
- replacedByTokenId: UUID (任意)
- userAgent: string (任意)
- ipAddress: string (任意)

## Article

- id: UUID
- userId: UUID
- url: string (必須、最大2048文字、ユーザー単位でユニーク)
- title: string (必須)
- summary: string (任意)
- thumbnailUrl: string (任意)
- status: enum(`UNREAD`, `READ`)
- readDate: date (任意)
- favorite: boolean
- rating: integer (`0` - `5`, 未指定時は `0`)
- notes: text (任意)
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
