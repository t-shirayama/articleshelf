# API仕様

## エンドポイント

### `GET /api/articles`

- 説明: 記事一覧を取得
- レスポンス: `id`, `url`, `title`, `summary`, `thumbnailUrl`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`, `createdAt`, `updatedAt`
- パラメータ: `status`, `tag`, `search`, `favorite`
- `status` は `UNREAD` または `READ`
- `tag` は単一タグ名で、大文字小文字を区別せず一致判定する
- `search` はタイトル、URL、概要、メモを対象に部分一致する
- `favorite` は `true` または `false`
- 備考: フロントエンドでは初回取得時に `status`, `search`, `favorite` をAPIへ渡し、複数タグ、おすすめ度、登録日範囲、既読日範囲、並び替えは取得後にフロントエンド側で適用する

### `GET /api/articles/{id}`

- 説明: 記事詳細を取得
- レスポンス: `id`, `url`, `title`, `summary`, `thumbnailUrl`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`, `createdAt`, `updatedAt`

### `POST /api/articles`

- 説明: 記事を追加
- リクエスト: `url`, `title`, `summary`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`
- `url` は必須かつURL形式
- `title` が未入力の場合は OGP タイトル、OGP タイトルがない場合は URL を使う
- `summary` が未入力の場合は OGP description を使う
- `status` が未指定の場合はドメイン側で未読扱いにする
- `favorite` が未指定の場合は `false`
- `rating` が未指定の場合は `0`
- `tags` は空白を除去し、空文字を除外して重複をまとめる
- `url` がアクセス不可、タイムアウト、または 4xx/5xx 応答の場合は保存しない

### `PUT /api/articles/{id}`

- 説明: 記事を更新
- リクエスト: `url`, `title`, `summary`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`
- `url` を変更する場合、変更先がアクセス不可、タイムアウト、または 4xx/5xx 応答なら保存しない
- `url` を変更した場合、または既存記事に `thumbnailUrl` がない場合は OGP を再取得してサムネイルURLを補完する
- `title`, `status`, `favorite`, `rating` が未指定の場合は既存値を維持する
- `tags` はリクエスト内容で置き換える

### `DELETE /api/articles/{id}`

- 説明: 記事を削除

### `GET /api/tags`

- 説明: タグ一覧を取得
- レスポンス: `id`, `name`, `createdAt`, `updatedAt`
- 備考: タグ名昇順で返す

### `POST /api/tags`

- 説明: タグを追加
- リクエスト: `name`
- `name` は必須

## エラーレスポンス

- バリデーションエラーや不正なパラメータは `400 Bad Request`
- アクセスできない URL は `400 Bad Request`
- 存在しない記事 ID は `404 Not Found`
- 重複 URL は `409 Conflict`
- エラー時のレスポンス形式は `timestamp` と `messages` を持つ JSON
- 重複 URL の場合は、登録済み記事の詳細へ遷移できるよう `existingArticleId` を返す
- `status` の不正値、`id` の不正な UUID、`readDate` の不正な日付形式も `messages` 配列に説明文を入れて返す
