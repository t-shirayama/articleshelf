# タグ API

## `GET /api/tags`

- 説明: タグ一覧を取得
- 認証: 必須
- レスポンス: `id`, `name`, `createdAt`, `updatedAt`, `articleCount`
- 備考: タグ名昇順で返す

## `POST /api/tags`

- 説明: タグを追加
- 認証: 必須
- リクエスト: `name`
- `name` は必須

## `PATCH /api/tags/{id}`

- 説明: タグ名を変更
- 認証: 必須
- リクエスト: `name`
- 同一ユーザー内で既存タグ名と重複する場合は `409 Conflict`
- 他ユーザーのタグ ID は `404 Not Found`

## `POST /api/tags/{sourceId}/merge`

- 説明: 元タグを統合先タグへ統合し、元タグを削除
- 認証: 必須
- リクエスト: `targetTagId`
- 元タグが付いていた記事は統合先タグへ付け替える
- 元タグと統合先タグの両方が付いている記事では重複紐づけを作らない
- 他ユーザーのタグ ID は `404 Not Found`
- レスポンス: `204 No Content`

## `DELETE /api/tags/{id}`

- 説明: 未使用タグを削除
- 認証: 必須
- 記事に紐づいているタグは削除できず `409 Conflict`
- 他ユーザーのタグ ID は `404 Not Found`
- レスポンス: `204 No Content`
