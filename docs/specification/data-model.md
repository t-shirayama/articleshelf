# データモデル

公開に向けたユーザー登録・ログイン追加時は、`User` と `RefreshToken` を追加し、`Article` と `Tag` はユーザーごとの所有データとして扱う方針です。
その場合、`Article.url` と `Tag.name` の一意性は全体一意ではなくユーザー単位の一意性に変更します。
詳細な認証・ユーザースコープ設計は `authentication.md` を参照してください。

## Article

- id: UUID
- url: string (必須、最大2048文字、ユニーク)
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
- name: string
- createdAt: timestamp
- updatedAt: timestamp

## ArticleTag

- articleId: UUID
- tagId: UUID
- 記事とタグは多対多で関連付ける
