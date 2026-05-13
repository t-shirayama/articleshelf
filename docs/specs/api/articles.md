# 記事 API

## `GET /api/articles`

- 説明: 記事一覧を取得
- 認証: 必須
- レスポンス: `id`, `version`, `url`, `title`, `summary`, `thumbnailUrl`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`, `createdAt`, `updatedAt`
- パラメータ: `status`, `tag`, `search`, `favorite`, `rating`, `createdFrom`, `createdTo`, `readFrom`, `readTo`, `page`, `size`, `sort`
- `status` は `UNREAD` または `READ`
- `tag` は repeatable query parameter として複数指定でき、OR 条件で大文字小文字を区別せず一致判定する
- `search` はタイトル、URL、概要、メモ、タグ名を対象に部分一致する
- `favorite` は `true` または `false`
- `rating` は repeatable query parameter として複数指定でき、指定したおすすめ度のいずれかに一致する記事を返す
- `createdFrom` / `createdTo` は登録日の下限 / 上限。`YYYY-MM-DD` で指定し、日付単位で inclusive に扱う
- `readFrom` / `readTo` は既読日の下限 / 上限。`YYYY-MM-DD` で指定し、未読記事は既読日 filter に一致しない
- `page` は 0 origin のページ番号。`page` または `size` を指定した場合、backend repository が PostgreSQL の LIMIT / OFFSET として適用する
- `size` は 1 - 200。未指定で page 指定がある場合は 50 件を既定値にする
- `sort` の許可値は `CREATED_DESC`, `CREATED_ASC`, `UPDATED_DESC`, `READ_DATE_DESC`, `TITLE_ASC`, `RATING_DESC`
- `sort` 未指定または未知値は `CREATED_DESC` として扱う
- tie-breaker は主 sort のあとに `createdAt desc`, `id desc` を基本とし、`CREATED_ASC` だけは `createdAt asc`, `id desc` を使う。`READ_DATE_DESC` は `readDate` の `NULL` を末尾へ寄せる
- `page` / `size` 未指定時は既存互換を優先して全件を返す
- 備考: 記事一覧画面は `status`, `search`, `favorite`, `tag`, `rating`, `createdFrom`, `createdTo`, `readFrom`, `readTo`, `sort`, `page`, `size` を server-driven query として使う。一方、カレンダーとサイドバー件数は現行互換のため別途全件 snapshot を取得している

## `GET /api/articles/{id}`

- 説明: 記事詳細を取得
- 認証: 必須
- レスポンス: `id`, `version`, `url`, `title`, `summary`, `thumbnailUrl`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`, `createdAt`, `updatedAt`
- 他ユーザーの記事 ID は `404 Not Found`

## `POST /api/articles`

- 説明: 記事を追加
- 認証: 必須
- リクエスト: `url`, `title`, `summary`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`
- レスポンスには optimistic locking 用の `version` を含め、作成直後は `0` から始まる
- `url` は必須かつURL形式、最大2048文字
- `title` は任意、最大255文字
- `title` が未入力の場合は OGP タイトル、OGP タイトルがない場合は URL を使う
- `summary` は任意、最大5000文字
- `summary` が未入力の場合は OGP description を使う
- `status` が未指定の場合はドメイン側で未読扱いにする
- `favorite` が未指定の場合は `false`
- `rating` は `0` - `5`、未指定の場合は `0`
- `notes` は任意、最大20000文字
- `tags` は最大20件、各タグ名は最大255文字。空白を除去し、空文字を除外して重複をまとめる
- `url` がアクセス不可、タイムアウト、または 4xx/5xx 応答の場合は保存しない

## `POST /api/articles/preview`

- 説明: 記事保存前に URL からプレビュー用メタデータを取得
- 認証: 必須
- リクエスト: `url`
- `url` は必須かつURL形式、最大2048文字
- 成功時は `200 OK` で `url`, `title`, `summary`, `thumbnailUrl`, `previewAvailable`, `errorReason` を返す
- OGP取得に成功した場合、`previewAvailable` は `true`、`errorReason` は `null`
- OGP取得不可、タイムアウト、または解析不可の場合も preview API としては `200 OK` を返し、`previewAvailable: false`, `errorReason: "OGP_FETCH_FAILED"` と空の `title`, `summary`, `thumbnailUrl` を返す
- `previewAvailable` はプレビュー情報として利用可能かを表し、保存可否の最終判断ではない。保存時は `POST /api/articles` が改めて URL 確認を行う
- 同一ユーザー内で重複 URL の場合は `409 Conflict` とし、共通エラー body の `existingArticleId` に登録済み記事 ID を含める

## `PUT /api/articles/{id}`

- 説明: 記事を更新
- 認証: 必須
- リクエスト: `version`, `url`, `title`, `summary`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`
- レスポンス: `id`, `version`, `url`, `title`, `summary`, `thumbnailUrl`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`, `createdAt`, `updatedAt`
- `version` は client が最後に読んだ記事 version を送る。server は保存成功時に increment 済み version を返す
- `url` は必須かつURL形式、最大2048文字
- `title` は任意、最大255文字
- `summary` は任意、最大5000文字
- `version` は必須、`0` 以上の整数
- `rating` は `0` - `5`
- `notes` は任意、最大20000文字
- `tags` は最大20件、各タグ名は最大255文字
- `url` を変更する場合、変更先がアクセス不可、タイムアウト、または 4xx/5xx 応答なら保存しない
- `url` を変更した場合、または既存記事に `thumbnailUrl` がない場合は OGP を再取得してサムネイルURLを補完する
- `title`, `status`, `favorite`, `rating` が未指定の場合は既存値を維持する
- `tags` はリクエスト内容で置き換える
- client の `version` が古い場合は `409 Conflict` と `ARTICLE_VERSION_CONFLICT` を返し、lost update を避ける

## `DELETE /api/articles/{id}`

- 説明: 記事を削除
- 認証: 必須
- 他ユーザーの記事 ID は `404 Not Found`

## Extension article API

Chrome 拡張機能向け API は通常 Web JWT ではなく、拡張機能専用 opaque token の `Authorization: Bearer <extensionToken>` だけを受け付ける。
scope と詳細な認証仕様は [Chrome 拡張機能仕様](../features/browser-extension.md) を正本とする。

### `GET /api/extension/articles/lookup?url=...`

- 説明: 現在 URL がログインユーザーの記事として登録済みか確認する
- 認証: 拡張機能 token の `article:lookup` scope が必須
- `url` は必須かつURL形式、最大2048文字
- 未登録または他ユーザーの記事は `404 Not Found`

### `POST /api/extension/articles`

- 説明: 現在 URL を記事として追加する
- 認証: 拡張機能 token の `article:create` scope が必須
- リクエスト: `url`, `title`, `status`, `readDate`
- `status` は `UNREAD` または `READ`
- `READ` の場合は `readDate` に `YYYY-MM-DD` を送る
- `UNREAD` の場合は `readDate: null`
- OGP 補完、URL 検証、重複 URL 判定、ユーザースコープは通常の `POST /api/articles` と同じ

### `PATCH /api/extension/articles/{id}/status`

- 説明: 登録済み記事の既読 / 未読状態だけを更新する
- 認証: 拡張機能 token の `article:update_status` scope が必須
- リクエスト: `status`, `readDate`
- title、summary、favorite、rating、notes、tags は既存値を維持する
- 他ユーザーの記事 ID は `404 Not Found`
