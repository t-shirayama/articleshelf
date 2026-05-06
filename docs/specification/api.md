# API仕様

## エンドポイント

### `GET /api/articles`

- 説明: 記事一覧を取得
- パラメータ: `status`, `tag`, `search`

### `GET /api/articles/{id}`

- 説明: 記事詳細を取得

### `POST /api/articles`

- 説明: 記事を追加
- リクエスト: `url`, `title`, `summary`, `status`, `readDate`, `favorite`, `notes`, `tags`

### `PUT /api/articles/{id}`

- 説明: 記事を更新

### `DELETE /api/articles/{id}`

- 説明: 記事を削除

### `GET /api/tags`

- 説明: タグ一覧を取得

### `POST /api/tags`

- 説明: タグを追加
