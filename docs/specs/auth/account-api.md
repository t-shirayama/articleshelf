# アカウント API

## `POST /api/auth/register`

- 認証: 不要
- rate limit: `IP` 単位。既定は `3回 / 600秒`
- request: `username`, `password`, `displayName`
- response: `user`, `accessToken`
- side effect: refresh cookie と CSRF cookie を設定
- `displayName` は任意。空または未指定の場合は正規化済み username を表示名として保存する
- error:
  - `400`: username 形式不正、password 要件不足、必須項目不足
  - `409`: username 重複
  - `429`: 登録試行回数の超過

## `POST /api/auth/login`

- 認証: 不要
- rate limit: `IP + username` 単位。username は trim / 小文字化後に扱う。既定は `5回 / 60秒`
- request: `username`, `password`
- response: `user`, `accessToken`
- side effect: refresh cookie と CSRF cookie を設定
- error:
  - `401`: username または password 不一致、ユーザー inactive
  - `429`: ログイン試行回数の超過

## `POST /api/auth/refresh`

- 認証: refresh cookie
- CSRF: `@CookieCsrfProtected`。`ARTICLESHELF_CSRF` cookie と `X-CSRF-Token` header の一致が必要
- response: `user`, `accessToken`
- side effect: refresh token rotation と cookie 再設定
- error:
  - `401`: refresh token 不正、期限切れ、失効済み、ユーザー inactive
  - `403`: CSRF token 不一致または欠落

## `POST /api/auth/logout`

- 認証: refresh cookie を主とする
- CSRF: `@CookieCsrfProtected`。`ARTICLESHELF_CSRF` cookie と `X-CSRF-Token` header の一致が必要
- response: `204 No Content`
- side effect: 現在端末の refresh token を失効し、cookie を削除する
- error:
  - `403`: CSRF token 不一致または欠落

## `POST /api/auth/logout-all`

- 認証: 必須
- CSRF: 不要。Bearer access token 認証のため `@CookieCsrfProtected` の対象外
- response: `204 No Content`
- side effect: 現在ユーザーの refresh token を全失効し、`token_valid_after` を更新して既発行 access token も無効化し、cookie を削除する

## `GET /api/users/me`

- 認証: 必須
- response: `id`, `username`, `displayName`, `roles`

## `GET /api/auth/me`

- 認証: 必須
- response: `id`, `username`, `displayName`, `roles`
- 位置づけ: `GET /api/users/me` と同じ現在ユーザー確認の互換エンドポイント。フロントエンドの通常導線は `GET /api/users/me` を使う

## `PATCH /api/users/me/password`

- 認証: 必須
- request: `currentPassword`, `newPassword`
- response: `204 No Content`
- side effect: password hash 更新、refresh token 全失効、`token_valid_after` 更新、cookie 削除
- frontend: 成功後はログイン画面に戻し、再ログインを促す

## `DELETE /api/users/me`

- 認証: 必須
- request: `currentPassword`
- response: `204 No Content`
- side effect: `users.status = DELETED`、refresh token 全失効、`token_valid_after` 更新、cookie 削除

## `POST /api/admin/users/{username}/password`

- 認証: `ADMIN` のみ
- request: `newPassword`
- response: `204 No Content`
- side effect: 対象ユーザーの password hash 更新、refresh token 全失効、`token_valid_after` 更新
- 新パスワードの本人通知はアプリ外の運用で行い、メール送信は実装しない

## PasswordPolicy

- 8文字以上、128文字以下
- username と同一不可
- 平文 password をログに出さない
- 登録、パスワード変更、管理者リセットで同じ `PasswordPolicy` を使う
- login では policy を再検証せず、保存済み hash との照合だけを行う
