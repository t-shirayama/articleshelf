# データモデル

## Article

- id: UUID
- url: string
- title: string
- summary: string (任意)
- status: enum(`UNREAD`, `READ`)
- readDate: date (任意)
- favorite: boolean
- notes: text
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
