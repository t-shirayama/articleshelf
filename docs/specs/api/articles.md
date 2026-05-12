# 記事 API

## `GET /api/articles`

- 説明: 記事一覧を取得
- 認証: 必須
- レスポンス: `id`, `url`, `title`, `summary`, `thumbnailUrl`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`, `createdAt`, `updatedAt`
- パラメータ: `status`, `tag`, `search`, `favorite`, `page`, `size`, `sort`
- `status` は `UNREAD` または `READ`
- `tag` は単一タグ名で、大文字小文字を区別せず一致判定する
- `search` はタイトル、URL、概要、メモを対象に部分一致する
- `favorite` は `true` または `false`
- `page` は 0 origin のページ番号。`page` または `size` を指定した場合、backend repository が PostgreSQL の LIMIT / OFFSET として適用する
- `size` は 1 - 200。未指定で page 指定がある場合は 50 件を既定値にする
- `sort` の許可値は `CREATED_DESC`, `CREATED_ASC`, `UPDATED_DESC`, `READ_DATE_DESC`, `TITLE_ASC`, `RATING_DESC`
- `sort` 未指定または未知値は `CREATED_DESC` として扱う
- tie-breaker は主 sort のあとに `createdAt desc`, `id desc` を基本とし、`CREATED_ASC` だけは `createdAt asc`, `id desc` を使う。`READ_DATE_DESC` は `readDate` の `NULL` を末尾へ寄せる
- `page` / `size` 未指定時は既存互換を優先して全件を返す
- 備考: フロントエンドでは初回取得時に `status`, `search`, `favorite` をAPIへ渡せる。複数タグ、おすすめ度、登録日範囲、既読日範囲、一覧 query state の接続は `server-driven-article-list-query` の実装を前提に段階移行する

## `GET /api/articles/{id}`

- 説明: 記事詳細を取得
- 認証: 必須
- レスポンス: `id`, `url`, `title`, `summary`, `thumbnailUrl`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`, `createdAt`, `updatedAt`
- 他ユーザーの記事 ID は `404 Not Found`

## `POST /api/articles`

- 説明: 記事を追加
- 認証: 必須
- リクエスト: `url`, `title`, `summary`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`
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
- リクエスト: `url`, `title`, `summary`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`
- `url` は必須かつURL形式、最大2048文字
- `title` は任意、最大255文字
- `summary` は任意、最大5000文字
- `rating` は `0` - `5`
- `notes` は任意、最大20000文字
- `tags` は最大20件、各タグ名は最大255文字
- `url` を変更する場合、変更先がアクセス不可、タイムアウト、または 4xx/5xx 応答なら保存しない
- `url` を変更した場合、または既存記事に `thumbnailUrl` がない場合は OGP を再取得してサムネイルURLを補完する
- `title`, `status`, `favorite`, `rating` が未指定の場合は既存値を維持する
- `tags` はリクエスト内容で置き換える

## `DELETE /api/articles/{id}`

- 説明: 記事を削除
- 認証: 必須
- 他ユーザーの記事 ID は `404 Not Found`
