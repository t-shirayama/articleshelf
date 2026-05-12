# Token / Cookie / CSRF

## 1. Access Token

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
access token の `iat` / `exp` は injected `Clock`、`jti` は injected `IdGenerator` に従って発行し、refresh token rotation と同じ deterministic testability 方針にそろえる。
独自の JWT 署名・payload parse 実装は持たない。

## 2. Refresh Token

- 形式: 256 bit の CSPRNG で生成した不透明 token
- 保存: `HMAC-SHA256(AUTH_REFRESH_TOKEN_HASH_SECRET, raw_refresh_token)` の結果を保存する
- 有効期限: 30日
- 送信方法: HttpOnly cookie
- rotation:
  - login/register 成功時に `family_id` を作成する
  - refresh 成功時は同じ `family_id` で新 token を発行し、旧 token を失効する
  - refresh token の検索は pessimistic write lock を取り、旧 token の `revoked_at is null` 条件付き update が成功した場合だけ replacement token を有効にする
  - 条件付き update が失敗した場合は refresh token replay / race として扱い、同一 family の未失効 token をすべて失効して refresh を失敗扱いにする
  - 失効済み token が再利用された場合も、同一 family の未失効 token をすべて失効する

## 3. Cookie / CSRF

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

refresh / CSRF cookie の発行と削除は `SessionCookieWriter` が担当する。cookie 認証を使う state-changing endpoint は `@CookieCsrfProtected` を付け、`CookieCsrfGuardInterceptor` が `CsrfTokenValidator` 経由で CSRF cookie と `X-CSRF-Token` header の一致を中央で検証する。Controller は request handling と認証ユースケース呼び出しに集中し、手動 `validate(...)` 呼び出しは追加しない。

CSRF 保護対象:

- `POST /api/auth/refresh`
- `POST /api/auth/logout`

CSRF 保護対象外:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout-all`
- Bearer access token を使う `/api/articles/**`、`/api/tags/**`、`/api/users/me/**`
