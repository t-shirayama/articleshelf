# 記事 API

## `GET /api/articles`

- 説明: 記事一覧を取得
- 認証: 必須
- レスポンス: `id`, `url`, `title`, `summary`, `thumbnailUrl`, `status`, `readDate`, `favorite`, `rating`, `notes`, `tags`, `createdAt`, `updatedAt`
- パラメータ: `status`, `tag`, `search`, `favorite`
- `status` は `UNREAD` または `READ`
- `tag` は単一タグ名で、大文字小文字を区別せず一致判定する
- `search` はタイトル、URL、概要、メモを対象に部分一致する
- `favorite` は `true` または `false`
- 備考: フロントエンドでは初回取得時に `status`, `search`, `favorite` をAPIへ渡し、複数タグ、おすすめ度、登録日範囲、既読日範囲、並び替えは取得後にフロントエンド側で適用する

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
