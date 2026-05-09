# ユーザー登録・ログイン・JWT認証仕様

最終更新: 2026-05-08

## 1. 目的

ArticleShelf はユーザーごとに記事、タグ、メモ、既読履歴を分離する。
認証はメール運用を前提にせず、`username + password` をログイン ID とする。

## 2. 方針サマリー

- 認証 ID は `username` とする
- メール確認、メール送信型パスワードリセット、SMTP 運用、メール文面管理は対象外
- API 認証は短命な JWT access token を使う
- refresh token は HttpOnly cookie として扱い、DB には HMAC hash のみ保存する
- access token はフロントエンドのメモリ上に保持し、localStorage には保存しない
- 記事、タグ、記事タグ紐づけは必ず `user_id` に紐づける
- 検索、詳細、更新、削除はすべて認証ユーザーの `user_id` でスコープする
- ユーザー間では同じ URL や同じタグ名を登録できる
- 退会は `users.status = DELETED` の論理削除とし、記事・タグは保持するが本人を含めて参照不可にする
- パスワード変更、退会、管理者リセットでは refresh token を全失効し、`token_valid_after` を更新する
- protected API は JWT の署名・期限だけでなく、ユーザーが `ACTIVE` であり token が `token_valid_after` より古くないことも確認する
- 通常起動では初期ユーザーを自動作成しない。初期 ADMIN が必要な検証環境だけ `ARTICLESHELF_INITIAL_USER_ENABLED=true` を明示する
- 登録 / ログインの公開 API は backend の in-memory レート制限で保護する。client IP は Spring が確定した remote address を使い、単一インスタンス向けの簡易制限とする。複数インスタンスでは共有ストア、proxy、WAF 側制限を別途使う

## 3. Username

username はログイン ID として使う。

- 登録・ログイン時は `trim` と小文字化を行う
- 3〜32文字
- 使用可能文字は `a-z`, `0-9`, `.`, `_`, `-`
- DB unique 制約は正規化後の `users.username` に適用する
- 表示名が未入力の場合は username を表示名として使う
- 既存 DB からの移行では `users.email` の local-part を元に username を生成し、重複時は suffix を付ける
- `users.email` は移行互換用の nullable legacy column とし、新規 API では返さない

## 4. Token

### 4.1 Access Token

- 形式: JWT
- 署名: HS256
- 有効期限: 15分
- 送信方法: `Authorization: Bearer <accessToken>`
- 主な claim:
  - `sub`: user id
  - `username`: 正規化済み username
  - `roles`: `["USER"]` または `["ADMIN"]`
  - `iat`: 発行時刻
  - `exp`: 失効時刻
  - `jti`: token id

`JwtTokenService` は Spring Security JOSE の `NimbusJwtEncoder` / `NimbusJwtDecoder` を使い、HS256 の発行・検証、期限、claim を検証する。
独自の JWT 署名・payload parse 実装は持たない。

### 4.2 Refresh Token

- 形式: 256 bit の CSPRNG で生成した不透明 token
- 保存: `HMAC-SHA256(AUTH_REFRESH_TOKEN_HASH_SECRET, raw_refresh_token)` の結果を保存する
- 有効期限: 30日
- 送信方法: HttpOnly cookie
- rotation:
  - login/register 成功時に `family_id` を作成する
  - refresh 成功時は同じ `family_id` で新 token を発行し、旧 token を失効する
  - 失効済み token が再利用された場合、同一 family の未失効 token をすべて失効する

### 4.3 Cookie / CSRF

local dev では `SameSite=Lax`, `Secure=false`, `AUTH_CSRF_ENABLED=false` を既定にする。
公開環境では refresh/logout が cookie 認証ベースになるため CSRF を必ず有効化する。

本番必須設定:

```text
AUTH_CSRF_ENABLED=true
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=None
FRONTEND_ORIGIN=https://your-frontend.example.com
JWT_ACCESS_SECRET=<long-random-secret>
AUTH_REFRESH_TOKEN_HASH_SECRET=<long-random-secret>
```

同一 site 配信なら `SameSite=Lax` も選択可能だが、production profile では CSRF は常に有効にする。

## 5. Account API

### `POST /api/auth/register`

- 認証: 不要
- rate limit: `IP` 単位。既定は `3回 / 600秒`
- request: `username`, `password`, `displayName`
- response: `user`, `accessToken`
- side effect: refresh cookie と CSRF cookie を設定
- error:
  - `400`: username 形式不正、password 要件不足、必須項目不足
  - `409`: username 重複
  - `429`: 登録試行回数の超過

### `POST /api/auth/login`

- 認証: 不要
- rate limit: `IP + username` 単位。username は trim / 小文字化後に扱う。既定は `5回 / 60秒`
- request: `username`, `password`
- response: `user`, `accessToken`
- side effect: refresh cookie と CSRF cookie を設定
- error:
  - `401`: username または password 不一致、ユーザー inactive
  - `429`: ログイン試行回数の超過

### `POST /api/auth/refresh`

- 認証: refresh cookie
- response: `user`, `accessToken`
- side effect: refresh token rotation と cookie 再設定
- error:
  - `401`: refresh token 不正、期限切れ、失効済み、ユーザー inactive

### `POST /api/auth/logout`

- 認証: refresh cookie を主とする
- response: `204 No Content`
- side effect: 現在端末の refresh token を失効し、cookie を削除する

### `POST /api/auth/logout-all`

- 認証: 必須
- response: `204 No Content`
- side effect: 現在ユーザーの refresh token を全失効し、`token_valid_after` を更新して既発行 access token も無効化し、cookie を削除する

### `GET /api/users/me`

- 認証: 必須
- response: `id`, `username`, `displayName`, `roles`

### `PATCH /api/users/me/password`

- 認証: 必須
- request: `currentPassword`, `newPassword`
- response: `204 No Content`
- side effect: password hash 更新、refresh token 全失効、`token_valid_after` 更新、cookie 削除
- frontend: 成功後はログイン画面に戻し、再ログインを促す

### `DELETE /api/users/me`

- 認証: 必須
- request: `currentPassword`
- response: `204 No Content`
- side effect: `users.status = DELETED`、refresh token 全失効、`token_valid_after` 更新、cookie 削除

### `POST /api/admin/users/{username}/password`

- 認証: `ADMIN` のみ
- request: `newPassword`
- response: `204 No Content`
- side effect: 対象ユーザーの password hash 更新、refresh token 全失効、`token_valid_after` 更新
- 新パスワードの本人通知はアプリ外の運用で行い、メール送信は実装しない

## 6. Data Model

### users

| カラム | 型 | 制約 | 説明 |
| --- | --- | --- | --- |
| id | UUID | PK | ユーザー ID |
| username | VARCHAR(32) | NOT NULL, UNIQUE | ログイン ID。小文字正規化 |
| email | VARCHAR | NULL | 移行互換用 legacy column。新規 API では返さない |
| password_hash | VARCHAR | NOT NULL | password hash |
| display_name | VARCHAR | NULL | 表示名 |
| role | VARCHAR | NOT NULL | `USER` または `ADMIN` |
| status | VARCHAR | NOT NULL | `ACTIVE`, `LOCKED`, `DELETED` |
| created_at | TIMESTAMP | NOT NULL | 登録日時 |
| updated_at | TIMESTAMP | NOT NULL | 更新日時 |
| last_login_at | TIMESTAMP | NULL | 最終ログイン日時 |
| token_valid_after | TIMESTAMP | NOT NULL | これより古い `iat` の access token を拒否する |

### refresh_tokens

| カラム | 型 | 制約 | 説明 |
| --- | --- | --- | --- |
| id | UUID | PK | refresh token ID |
| user_id | UUID | FK, NOT NULL | users.id |
| token_hash | VARCHAR | NOT NULL, UNIQUE | refresh token の hash |
| family_id | UUID | NOT NULL | rotation 系列 |
| expires_at | TIMESTAMP | NOT NULL | 有効期限 |
| revoked_at | TIMESTAMP | NULL | 失効日時 |
| created_at | TIMESTAMP | NOT NULL | 発行日時 |
| replaced_by_token_id | UUID | NULL | rotation 後の token |
| user_agent | VARCHAR | NULL | 端末識別補助 |
| ip_address | VARCHAR | NULL | 監査補助 |

## 7. PasswordPolicy

- 8文字以上、128文字以下
- username と同一不可
- 平文 password をログに出さない
- 登録、パスワード変更、管理者リセットで同じ `PasswordPolicy` を使う
- login では policy を再検証せず、保存済み hash との照合だけを行う

## 8. Protected API

記事・タグ API はすべて認証必須にする。

- `Authorization` header の access token を検証する
- `sub` の user id を application service へ渡す
- 一覧は `user_id = currentUser.id` の記事のみ返す
- 詳細、更新、削除は `id` と `user_id` の両方で検索する
- 他ユーザーの記事 ID / タグ ID は存在しないものとして `404 Not Found` を返す
- application 層の検証に加え、`article_tags` の複合 FK で article / tag の user mismatch を拒否する

## 9. Frontend

- `features/auth` は username-based API adapter と Pinia store を持つ
- `AuthScreen` は username、表示名、password を扱う
- `AppSidebar` からアカウント設定ダイアログを開ける
- アカウント設定ダイアログではパスワード変更、全端末ログアウト、退会を扱う
- パスワード変更、全端末ログアウト、退会の成功時は auth state と article state をクリアし、ログイン画面へ戻す
- 管理者パスワードリセットは API のみを提供し、管理者 UI は初期スコープ外とする

## 10. 初期データ

- 通常の `docker compose up --build` ではユーザー、記事、タグを自動投入しない
- `ARTICLESHELF_INITIAL_USER_ENABLED=true` の場合だけ、起動時に `ARTICLESHELF_INITIAL_USERNAME` / `ARTICLESHELF_INITIAL_USER_PASSWORD` の ADMIN ユーザーを作成または再利用する
- 起動時初期ユーザー作成は管理者リセット検証や legacy 開発 DB の所有者補完用であり、通常利用のデモデータ投入には使わない
- E2E はテストごとに一意 username / URL を作成し、既存データには依存しない

## 11. テスト観点

- username 正規化、形式不正、重複を検証する
- password が username と同一の場合に拒否する
- username 登録、login、refresh、logout の一連フローを検証する
- パスワード変更、全端末ログアウト、退会、管理者リセットで refresh token が全失効することを検証する
- 全端末ログアウト後は、他端末を含む既発行 access token が protected API で `401` になることを検証する
- `token_valid_after` より古い JWT と `DELETED` user の JWT を拒否する
- 退会後に login、refresh、protected API が使えないことを検証する
- `ADMIN` のみ管理者パスワードリセットを実行できることを検証する
- フロントエンドでは auth API adapter、auth store、アカウント操作後の state clear を検証する
- E2E では username 登録/ログイン、パスワード変更、全端末ログアウト、退会、管理者リセットを確認する

## 12. 未決事項

- access token 即時失効の秒単位精度で足りない場合に token version 方式へ拡張するか
- 退会済みユーザーのデータを一定期間後に物理削除するか
- `displayName` 変更 API を追加するか
