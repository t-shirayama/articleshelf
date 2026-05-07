# API仕様

## 認証方針

現行 API はユーザー登録・ログイン・JWT 認証を必須とします。
記事 API / タグ API はすべて `Authorization: Bearer <accessToken>` が必要で、JWT の `sub` から確定したユーザー ID で記事・タグ・重複 URL 判定をスコープします。
ログイン状態の復元には HttpOnly refresh cookie を使い、フロントエンドは access token をメモリ上に保持します。
詳細は `authentication.md` を参照してください。

### 認証 API

#### `POST /api/auth/register`

- 説明: ユーザー登録
- リクエスト: `email`, `password`, `displayName`
- レスポンス: `user`, `accessToken`
- 副作用: `READSTACK_REFRESH` と `READSTACK_CSRF` cookie を設定

#### `POST /api/auth/login`

- 説明: ログイン
- リクエスト: `email`, `password`
- レスポンス: `user`, `accessToken`
- 副作用: `READSTACK_REFRESH` と `READSTACK_CSRF` cookie を設定

#### `POST /api/auth/refresh`

- 説明: refresh cookie から access token を再発行
- レスポンス: `user`, `accessToken`
- 副作用: refresh token を rotation し、cookie を再設定

#### `POST /api/auth/logout`

- 説明: 現在端末の refresh token を失効してログアウト
- レスポンス: `204 No Content`
- 副作用: session cookie を削除

#### `GET /api/users/me`

- 説明: 現在のログインユーザーを取得
- レスポンス: `id`, `email`, `displayName`, `roles`

## エンドポイント

### `GET /api/articles`

- 説明: 記事一覧を取得
- 認証: 必須
- レスポンス: `id`, `url`, `title`, `summary`, `thumbnailUrl`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`, `createdAt`, `updatedAt`
- パラメータ: `status`, `tag`, `search`, `favorite`
- `status` は `UNREAD` または `READ`
- `tag` は単一タグ名で、大文字小文字を区別せず一致判定する
- `search` はタイトル、URL、概要、メモを対象に部分一致する
- `favorite` は `true` または `false`
- 備考: フロントエンドでは初回取得時に `status`, `search`, `favorite` をAPIへ渡し、複数タグ、おすすめ度、登録日範囲、既読日範囲、並び替えは取得後にフロントエンド側で適用する

### `GET /api/articles/{id}`

- 説明: 記事詳細を取得
- 認証: 必須
- レスポンス: `id`, `url`, `title`, `summary`, `thumbnailUrl`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`, `createdAt`, `updatedAt`
- 他ユーザーの記事 ID は `404 Not Found`

### `POST /api/articles`

- 説明: 記事を追加
- 認証: 必須
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
- 認証: 必須
- リクエスト: `url`, `title`, `summary`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`
- `url` を変更する場合、変更先がアクセス不可、タイムアウト、または 4xx/5xx 応答なら保存しない
- `url` を変更した場合、または既存記事に `thumbnailUrl` がない場合は OGP を再取得してサムネイルURLを補完する
- `title`, `status`, `favorite`, `rating` が未指定の場合は既存値を維持する
- `tags` はリクエスト内容で置き換える

### `DELETE /api/articles/{id}`

- 説明: 記事を削除
- 認証: 必須
- 他ユーザーの記事 ID は `404 Not Found`

### `GET /api/tags`

- 説明: タグ一覧を取得
- 認証: 必須
- レスポンス: `id`, `name`, `createdAt`, `updatedAt`, `articleCount`
- 備考: タグ名昇順で返す

### `POST /api/tags`

- 説明: タグを追加
- 認証: 必須
- リクエスト: `name`
- `name` は必須

### `PATCH /api/tags/{id}`

- 説明: タグ名を変更
- 認証: 必須
- リクエスト: `name`
- 同一ユーザー内で既存タグ名と重複する場合は `409 Conflict`
- 他ユーザーのタグ ID は `404 Not Found`

### `POST /api/tags/{sourceId}/merge`

- 説明: 元タグを統合先タグへ統合し、元タグを削除
- 認証: 必須
- リクエスト: `targetTagId`
- 元タグが付いていた記事は統合先タグへ付け替える
- 元タグと統合先タグの両方が付いている記事では重複紐づけを作らない
- 他ユーザーのタグ ID は `404 Not Found`
- レスポンス: `204 No Content`

### `DELETE /api/tags/{id}`

- 説明: 未使用タグを削除
- 認証: 必須
- 記事に紐づいているタグは削除できず `409 Conflict`
- 他ユーザーのタグ ID は `404 Not Found`
- レスポンス: `204 No Content`

## エラーレスポンス

- フロントエンドは API リクエストへ現在の表示言語を `Accept-Language` として付与する
- API エラー文言は `Accept-Language` に応じて日本語 / 英語で返す
- `Accept-Language` が `ja` 以外、未対応、または未指定の場合は英語で返す
- バリデーションエラーや不正なパラメータは `400 Bad Request`
- 未認証または token 不正は `401 Unauthorized`
- CSRF token 不一致は `403 Forbidden`
- アクセスできない URL は `400 Bad Request`
- 存在しない記事 ID は `404 Not Found`
- 重複 URL は `409 Conflict`
- 重複タグ名、使用中タグの削除は `409 Conflict`
- エラー時のレスポンス形式は `timestamp` と `messages` を持つ JSON
- エラー時の JSON 形状は言語に関係なく維持し、`messages` の本文だけを切り替える
- 重複 URL の場合は、登録済み記事の詳細へ遷移できるよう `existingArticleId` を返す
- `status` の不正値、`id` の不正な UUID、`readDate` の不正な日付形式も `messages` 配列に説明文を入れて返す
