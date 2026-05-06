# API仕様

## エンドポイント

### `GET /api/articles`

- 説明: 記事一覧を取得
- レスポンス: `id`, `url`, `title`, `summary`, `thumbnailUrl`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`, `createdAt`, `updatedAt`
- パラメータ: `status`, `tag`, `search`, `favorite`
- `status` は `UNREAD` または `READ`
- `favorite` は `true` または `false`
- 備考: 並び替えは現状フロントエンド側で実施する

### `GET /api/articles/{id}`

- 説明: 記事詳細を取得
- レスポンス: `id`, `url`, `title`, `summary`, `thumbnailUrl`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`, `createdAt`, `updatedAt`

### `POST /api/articles`

- 説明: 記事を追加
- リクエスト: `url`, `title`, `summary`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`

### `PUT /api/articles/{id}`

- 説明: 記事を更新
- リクエスト: `url`, `title`, `summary`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`

### `DELETE /api/articles/{id}`

- 説明: 記事を削除

### `GET /api/tags`

- 説明: タグ一覧を取得

### `POST /api/tags`

- 説明: タグを追加

## エラーレスポンス

- バリデーションエラーや不正なパラメータは `400 Bad Request`
- 存在しない記事 ID は `404 Not Found`
- 重複 URL は `409 Conflict`
- エラー時のレスポンス形式は `timestamp` と `messages` を持つ JSON
- `status` の不正値、`id` の不正な UUID、`readDate` の不正な日付形式も `messages` 配列に説明文を入れて返す
