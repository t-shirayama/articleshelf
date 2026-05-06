# データモデル

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
