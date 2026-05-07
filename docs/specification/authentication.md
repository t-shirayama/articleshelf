# ユーザー登録・ログイン・JWT認証設計

最終更新: 2026-05-07

## 1. 目的

ReadStack を公開アプリとして提供するため、ユーザー登録、ログイン、ログアウト、JWT 認証、ユーザーごとのデータ分離を追加する。
この設計の最重要要件は「ユーザーは自身が登録した記事、タグ、メモ、既読履歴のみ参照・更新できる」ことである。

## 2. 方針サマリー

- 認証方式はメールアドレス + パスワードを MVP とする
- API 認証は JWT access token を使う
- refresh token は HttpOnly cookie として扱い、漏えいリスクを下げる
- access token は短命にし、フロントエンドのメモリ上に保持する
- 記事、タグは必ず `user_id` に紐づける
- 現行実装では `article_tags` は既存の `article_id` / `tag_id` join table のままとし、application / repository 層で article / tag の user mismatch を防ぐ
- `article_tags.user_id` と複合 FK による DB レベルの user mismatch 防止は、本番 migration 導入時の必須フォローとする
- API の検索・詳細・更新・削除はすべて認証ユーザーの `user_id` でスコープする
- ユーザー間で同じ URL や同じタグ名を登録できる
- local dev では `SameSite=Lax`, `Secure=false`, CSRF 無効を既定とする
- 公開環境で cross-site 構成にする場合は `SameSite=None; Secure` と CSRF token を必須にする
- 現行実装は `ddl-auto=update` を維持し、起動時に初期 owner ユーザーへ既存データの owner を付与する

## 3. 認証方式の検討

| 方式 | 概要 | 長所 | 短所 | 採用判断 |
| --- | --- | --- | --- | --- |
| Session cookie | サーバー側 session を保持 | 失効制御しやすい | サーバー状態が必要、スケール時に共有 store が必要 | 将来 BFF 化する場合に再検討 |
| JWT access token only | JWT のみで認証 | 実装が単純 | 盗難時に失効しづらく、長寿命にしがち | 不採用 |
| JWT access + refresh cookie | 短命 access token と refresh cookie | SPA と REST API の分離に合う、盗難影響を短くできる | refresh / CSRF 設計が必要 | MVP 採用 |
| 外部 IdP | Google/GitHub/Supabase Auth 等 | セキュリティ機能が豊富 | 初期設計が外部依存になる | 将来候補 |

## 4. Token 設計

### 4.1 Access Token

- 形式: JWT
- 署名: HS256 または RS256
- MVP では運用を単純にするため HS256 を採用し、`JWT_ACCESS_SECRET` を十分長いランダム値にする
- 有効期限: 15 分
- 保持場所: フロントエンドのメモリ上
- 送信方法: `Authorization: Bearer <accessToken>`
- 主な claim:
  - `sub`: user id
  - `email`: メールアドレス
  - `roles`: `["USER"]`
  - `iat`: 発行時刻
  - `exp`: 失効時刻
  - `jti`: token id
- roles の扱い:
  - JWT claim では `roles` を文字列配列として保持する
  - Spring Security では `roles` の各値に `ROLE_` prefix を付与し、`ROLE_USER` の `GrantedAuthority` に変換する
  - claim 自体には `ROLE_USER` ではなく `USER` を保存する
- `jti` の扱い:
  - MVP では `jti` を発行するが、access token blacklist は持たない
  - `jti` は認証可否の判定には使わず、監査ログ、障害調査、将来の即時失効機能のための識別子として扱う
  - 監査ログには `jti`, `sub`, request path, result を記録できるようにする
  - 将来、パスワード変更時の access token 即時失効を実装する場合は、`jti` blacklist または `users.token_valid_after` のような token version / valid-after 方式を追加する

### 4.2 Refresh Token

- 形式: 256 bit の CSPRNG で生成したランダムな不透明 token
- 表現: base64url など URL / cookie safe な文字列
- 保存: DB には `HMAC-SHA256(AUTH_REFRESH_TOKEN_HASH_SECRET, raw_refresh_token)` の結果を保存する
- password と異なり refresh token は高エントロピーなサーバー生成乱数であるため、MVP では BCrypt ではなく HMAC-SHA256 を使う
- 有効期限: 30 日
- 送信方法: HttpOnly cookie
- Cookie 属性:
  - `HttpOnly`
  - `Secure`
  - local dev では `SameSite=Lax`, `Secure=false` を既定とする
  - 公開環境で frontend と API が cross-site になる場合は `SameSite=None; Secure` に切り替える
  - `SameSite=None` 運用では refresh / logout に CSRF token を必須とする
- rotation:
  - `family_id` はログイン、ユーザー登録直後の自動ログイン、端末ごとの新規 session 作成時に UUID として生成する
  - refresh 成功時に発行する新 token は、旧 token と同じ `family_id` を引き継ぐ
  - refresh 成功時に新しい refresh token を発行する
  - 旧 refresh token は即時失効する
  - 失効済み refresh token が再利用された場合は、同一 user の refresh token family を全失効する
- refresh 処理の手順:
  1. cookie の refresh token を `AUTH_REFRESH_TOKEN_HASH_SECRET` で HMAC-SHA256 hash 化し、`refresh_tokens.token_hash` で検索する
  2. token が存在しない、期限切れ、user が `ACTIVE` でない場合は `401` を返す
  3. `revoked_at` が設定済みの場合は token reuse とみなし、`UPDATE refresh_tokens SET revoked_at = now() WHERE user_id = :userId AND family_id = :familyId AND revoked_at IS NULL` を実行して同一 family の未失効 token をすべて失効し、`401` を返す
  4. 正常な token の場合は新 refresh token を生成し、同じ `family_id` で保存する
  5. 旧 token は `revoked_at = now()`, `replaced_by_token_id = :newTokenId` に更新する
  6. 新 refresh token cookie と新 access token を返す
- token reuse 検知のため、失効済み token record は有効期限を過ぎるまで削除しない

### 4.3 Token 失効

- ログアウト時は refresh token を DB 上で失効し、cookie を削除する
- access token は短命のためサーバー側 blacklist は MVP では持たない
- パスワード変更、漏えい疑い、退会時は当該ユーザーの refresh token を全失効する

## 5. データモデル

### 5.1 users

| カラム | 型 | 制約 | 説明 |
| --- | --- | --- | --- |
| id | UUID | PK | ユーザー ID |
| email | VARCHAR | NOT NULL, UNIQUE | ログイン ID。小文字正規化 |
| password_hash | VARCHAR | NOT NULL | BCrypt などで hash 化 |
| display_name | VARCHAR | NULL | 表示名 |
| role | VARCHAR | NOT NULL | MVP は `USER` |
| status | VARCHAR | NOT NULL | `ACTIVE`, `LOCKED`, `DELETED` |
| created_at | TIMESTAMP | NOT NULL | 登録日時 |
| updated_at | TIMESTAMP | NOT NULL | 更新日時 |
| last_login_at | TIMESTAMP | NULL | 最終ログイン日時 |

email 正規化:

- 登録・ログイン時は `trim` 後に `Locale.ROOT` 相当で小文字化する
- MVP では Unicode 正規化、Gmail の dot 除去、plus addressing 除去は行わない
- DB unique 制約は正規化後 email に対して適用する

ユーザー status:

- `ACTIVE`: ログイン、refresh、通常 API 利用が可能
- `LOCKED`: ログイン、refresh、protected API 利用を拒否する。既存 refresh token は全失効する
- `DELETED`: 退会済みとしてログイン、refresh、protected API 利用を拒否する。既存 refresh token は全失効する
- MVP では退会時に user を `DELETED` とし、記事・タグは user scope により参照不可にする。記事・タグの物理削除タイミングは未決事項で管理する

### 5.2 refresh_tokens

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

### 5.3 articles 変更

| 変更 | 内容 |
| --- | --- |
| 追加 | `user_id UUID`。現行 `ddl-auto` 実装では既存データ移行のため nullable とし、application 層で必須設定する。本番 migration で `NOT NULL` 化する |
| unique | `url` 単独 unique を廃止し、`(user_id, url)` unique に変更 |
| unique | article_tags の複合 FK 用に `(user_id, id)` unique を追加 |
| index | `(user_id, status)`, `(user_id, favorite)`, `(user_id, created_at)` |

記事削除:

- MVP の通常の記事削除は物理削除とし、対象 article は `id` と `user_id` の両方で特定する
- article 削除時は関連する `article_tags` を同時に削除する
- ユーザー退会時の全記事削除は MVP の通常操作には含めず、`users.status = DELETED` で参照不可にする

### 5.4 tags 変更

| 変更 | 内容 |
| --- | --- |
| 追加 | `user_id UUID`。現行 `ddl-auto` 実装では既存データ移行のため nullable とし、application 層で必須設定する。本番 migration で `NOT NULL` 化する |
| unique | `name` 単独 unique ではなく `(user_id, lower(name))` 相当 |
| unique | article_tags の複合 FK 用に `(user_id, id)` unique を追加 |
| index | `(user_id, name)` |

タグ削除:

- MVP ではタグ単体削除 API は必須にしない
- タグ削除を追加する場合は `id` と `user_id` の両方で特定し、関連する `article_tags` を同時に削除する
- ユーザー退会時の全タグ削除は MVP の通常操作には含めず、`users.status = DELETED` で参照不可にする

### 5.5 article_tags

記事とタグが同一ユーザーに属することを application 層で必ず検証する。
検証タイミングは article と tag を紐づける直前とする。
現行実装では `article_tags` に `user_id` をまだ持たせていない。
本番 migration 導入時は DB 層でも user mismatch を防ぐため、`article_tags` に `user_id` を持たせる。

推奨DDL:

```sql
CREATE TABLE article_tags (
  user_id UUID NOT NULL,
  article_id UUID NOT NULL,
  tag_id UUID NOT NULL,
  PRIMARY KEY (user_id, article_id, tag_id),
  FOREIGN KEY (user_id, article_id) REFERENCES articles(user_id, id),
  FOREIGN KEY (user_id, tag_id) REFERENCES tags(user_id, id)
);
```

前提制約:

- `articles` は `UNIQUE (user_id, id)` を持つ
- `tags` は `UNIQUE (user_id, id)` を持つ
- `article_tags.user_id` は current user id を保存する
- application bug で別ユーザーの article と tag を混在させても、DB の複合 FK で保存を拒否する

- `addArticle(CurrentUser user, AddArticleCommand command)`:
  - tag name を受け取り、`findByUserIdAndLowerName(user.id, name)` で既存タグを取得する
  - 存在しない tag name は `user.id` を owner として作成する
  - article は `user.id` を owner として作成する
  - article_tags 登録前に、article.userId と tag.userId がどちらも `user.id` と一致することを検証する
- `updateArticle(CurrentUser user, UUID articleId, UpdateArticleCommand command)`:
  - article は `findByIdAndUserId(articleId, user.id)` で取得する
  - tag name の解決・作成は `addArticle` と同じ user scope で行う
  - 紐づけ差し替え前に、すべての tag.userId が article.userId と一致することを検証する
- `addTag(CurrentUser user, AddTagCommand command)`:
  - tag は必ず `user.id` を owner として作成する
  - 他ユーザーの tag name 重複は許可する
- 将来、client から tag ID を受け取る API を追加する場合は、必ず `findTagByIdAndUserId(tagId, user.id)` で取得し、見つからない場合は `404` とする

現行実装では `JpaArticleRepository.resolveTagEntities` で `article.userId` と `tag.userId` の一致を検証する。
DB 制約はまだ最後の防御線になっていないため、`article_tags.user_id` と複合 FK の導入を本番 migration 前の必須残課題として扱う。

## 6. API 仕様

### 6.1 Public API

#### `POST /api/auth/register`

- 説明: ユーザー登録
- 認証: 不要
- リクエスト:
  - `email`
  - `password`
  - `displayName`
- レスポンス:
  - `user`
  - `accessToken`
- 副作用:
  - refresh token cookie を設定
- エラー:
  - `400`: メール形式不正、パスワード要件不足
  - `409`: メールアドレス重複

#### `POST /api/auth/login`

- 説明: ログイン
- 認証: 不要
- リクエスト:
  - `email`
  - `password`
- レスポンス:
  - `user`
  - `accessToken`
- 副作用:
  - refresh token cookie を設定
- エラー:
  - `401`: 認証失敗

#### `POST /api/auth/refresh`

- 説明: refresh cookie から access token を再発行
- 認証: refresh token cookie
- レスポンス:
  - `user`
  - `accessToken`
- 副作用:
  - refresh token を rotation し、新 cookie を設定
- エラー:
  - `401`: refresh token 不正、期限切れ、失効済み
  - `401` 時は refresh cookie と CSRF cookie を削除する `Set-Cookie` も返す

#### `POST /api/auth/logout`

- 説明: ログアウト
- 認証: refresh token cookie を主とする
- CSRF: cross-site cookie 運用では `X-CSRF-Token` 必須
- レスポンス: `204 No Content`
- 副作用:
  - refresh cookie に対応する現在端末の refresh token を失効
  - cookie を削除
- 補足:
  - MVP の `/api/auth/logout` は現在端末だけをログアウトする
  - access token が有効でも refresh cookie がない場合は、cookie 削除 Set-Cookie を返して `204` とする
  - 全端末ログアウトは `POST /api/auth/logout-all` として将来追加する
  - パスワード変更、漏えい疑い、退会時は API を分けず application service から当該 user の refresh token を全失効する

### 6.2 Protected API

既存の記事・タグ API はすべて認証必須に変更する。

- `GET /api/articles`
- `GET /api/articles/{id}`
- `POST /api/articles`
- `PUT /api/articles/{id}`
- `DELETE /api/articles/{id}`
- `GET /api/tags`
- `POST /api/tags`

共通ルール:

- `Authorization` header の access token を検証する
- `sub` の user id を application service へ渡す
- 一覧は `user_id = currentUser.id` の記事のみ返す
- 詳細、更新、削除は `id` と `user_id` の両方で検索する
- 他ユーザーの記事 ID は存在しないものとして `404 Not Found` を返す方針を基本にする
- 他ユーザーのタグ ID / タグ名を記事へ紐づけない

### 6.3 Current User API

#### `GET /api/users/me`

- 説明: 現在のログインユーザー情報を取得
- 認証: 必須
- レスポンス:
  - `id`
  - `email`
  - `displayName`
  - `roles`

## 7. バックエンド設計

### 7.1 依存追加

`backend/pom.xml` に追加済み:

- `spring-boot-starter-security`
- JWT は外部ライブラリを追加せず、`JwtTokenService` で HS256 の発行 / 検証を実装している
- migration 導入時は `flyway-core` を追加候補とする

### 7.2 パッケージ構成

- `com.readstack.domain.user`
  - `PasswordPolicy`
  - `PasswordPolicyException`
  - `UserStatus`
- `com.readstack.application.auth`
  - `AuthService`
  - `AuthResponse`
  - `AuthResult`
  - `CurrentUser`
  - `RefreshSession`
  - `UserResponse`
- `com.readstack.infrastructure.security`
  - `JwtTokenService`
  - `PasswordEncoderConfig`
  - `SecurityConfig`
  - `JwtAuthenticationFilter`
  - `RefreshTokenHashService`
- `com.readstack.infrastructure.persistence`
  - `UserEntity`
  - `RefreshTokenEntity`
  - `SpringDataUserJpaRepository`
  - `SpringDataRefreshTokenJpaRepository`
- `com.readstack.adapter.web`
  - `AuthController`
  - `UserController`

### 7.3 Spring Security

- `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `/actuator/health` は認証不要
- `/api/**` は認証必須
- CSRF:
  - Bearer token API は CSRF 無効
  - local dev では `AUTH_CSRF_ENABLED=false` を既定とする
  - cross-site cookie を使う公開環境では、refresh / logout に double submit cookie 方式の CSRF token を必須にする
  - login / register / refresh 成功時に、HttpOnly ではない `READSTACK_CSRF` cookie を発行する
  - frontend は refresh / logout で `READSTACK_CSRF` cookie の値を `X-CSRF-Token` header に設定する
  - backend は cookie 値と header 値の一致を確認し、一致しない場合は `403` を返す
  - CSRF token は refresh token rotation と同時に再発行する
- CORS:
  - `FRONTEND_ORIGIN` を本番 URL に設定
  - cookie を扱う場合は `allowCredentials(true)`
  - `Authorization`, `Content-Type`, `X-CSRF-Token` を許可
- 例外:
  - 未認証は `401`
  - 権限不足は `403`
  - 他ユーザーデータ参照は原則 `404`

### 7.4 Application Service の変更

現在の `ArticleService` は user context を受け取っていない。
認証追加時は、次のように current user を明示的に渡す。

- `findArticles(CurrentUser user, ArticleQuery query)`
- `findArticle(CurrentUser user, UUID articleId)`
- `addArticle(CurrentUser user, AddArticleCommand command)`
- `updateArticle(CurrentUser user, UUID articleId, UpdateArticleCommand command)`
- `deleteArticle(CurrentUser user, UUID articleId)`
- `findTags(CurrentUser user)`
- `addTag(CurrentUser user, AddTagCommand command)`

Controller では `@AuthenticationPrincipal` から user id を取り出し、application 層へ渡す。
Repository には `findByIdAndUserId`, `findByUrlAndUserId`, `findAllByUserId` を追加する。
記事とタグの紐づけは service 内で user scope 済みの entity だけを使い、article_tags へ保存する直前に article.userId と tag.userId の一致を検証する。
不一致を検知した場合はアプリケーション例外として処理し、通常の user 操作では到達しない内部不整合としてログに残す。

### 7.5 PasswordPolicy

パスワード要件の責務は `PasswordPolicy` に集約する。

- Controller / request DTO は JSON 形式、必須項目、文字列長の上限など入力形式の検証だけを行う
- `AuthService.register` は email を小文字正規化した後、password hash 化の前に `PasswordPolicy.validate(email, password)` を呼ぶ
- `PasswordPolicy` はセクション9.1の要件を検証し、登録 API の `400` 応答に変換できる validation error を返す
- login では password policy を再検証せず、保存済み hash との照合だけを行う
- パスワード変更機能を追加する場合も同じ `PasswordPolicy` を使う

## 8. フロントエンド設計

### 8.1 画面

- 登録画面
- ログイン画面
- ログアウト導線
- ログイン後の ReadStack workspace
- 未ログイン時の保護 route redirect

### 8.2 状態管理

`features/auth` を追加済み。

- `api`
  - `register`
  - `login`
  - `refresh`
  - `logout`
  - `fetchCurrentUser`
- `store`
  - `user`
  - `accessToken`
  - `isAuthenticated`
  - `authReady`
  - `login`
  - `register`
  - `refreshSession`
  - `logout`
- `components`
  - `AuthScreen`

Access token は `localStorage` に保存せず、Pinia store のメモリ上に保持する。
画面 reload 時は `/api/auth/refresh` を呼び、refresh cookie が有効なら access token を再取得する。

### 8.3 API client

- request interceptor 相当で `Authorization: Bearer` を付与する
- `401` を受けたら一度だけ refresh を試す
- refresh 成功後に元リクエストを再送する
- refresh 失敗時は auth store をクリアし、ログイン画面へ遷移する
- 複数リクエストが同時に `401` になった場合は refresh を 1 回にまとめる
- access token の `exp` を decode し、期限の 60 秒前を目安に proactive refresh を試す
- proactive refresh が失敗した場合は auth store をクリアし、ログイン画面へ遷移する
- network 一時不調などで proactive refresh が実行できなかった場合に備え、`401` 時の refresh / retry は fallback として残す

### 8.4 UX

- 登録後は空の一覧画面へ遷移し、記事追加を促す
- ログイン失敗時はフォーム内にエラーを表示し、入力内容は保持する
- ログアウト後は access token と記事 store を破棄する
- ユーザー切り替え時に前ユーザーの記事一覧が一瞬表示されないよう、auth 変更時に article store を初期化する
- セッション切れ時は操作中のフォームを失いにくいよう、保存直前の `401` では refresh 後に再送する

## 9. セキュリティ要件

### 9.0 メールアドレス

- 登録・ログイン・検索用の email は `trim` と小文字化を行う
- Unicode 正規化、Gmail の dot / plus 正規化は MVP では行わない
- 正規化後の email を `users.email` に保存し、unique 制約も正規化後の値に適用する

### 9.1 パスワード

- 8 文字以上、128 文字以下
- メールアドレスと同一不可
- BCrypt など adaptive hash を使う
- 平文 password をログに出さない
- 登録・ログイン API は rate limit の導入を検討する
- これらの要件は `PasswordPolicy` で検証し、`RegisterCommand` は raw password を hash 化前に `PasswordPolicy` へ渡す

### 9.2 エラー応答

- ログイン失敗では「メールアドレスまたはパスワードが正しくありません」のように曖昧にする
- 登録時のメール重複は UX 優先で `409` として返す
- 認証失敗の詳細を client へ出しすぎない

### 9.3 ユーザースコープ

- application 層で user id を必須引数にする
- repository query で user id 条件を必ず含める
- DB unique 制約も user id を含める
- E2E / IT にユーザー分離ケースを必ず入れる

### 9.4 Cookie / CORS

無料枠の初期構成では frontend と backend が異なる origin になる可能性が高い。
現行実装は local dev を既定値として、公開環境では環境変数で cross-site 構成へ切り替える。
cross-site 構成では次を必須とする。

- HTTPS 必須
- refresh cookie は `Secure`
- refresh cookie は `SameSite=None`
- `Access-Control-Allow-Credentials` を有効化
- `Access-Control-Allow-Origin` は `*` にしない
- refresh / logout には double submit cookie 方式の CSRF token を必須にする
- refresh / logout は `X-CSRF-Token` がない、または CSRF cookie と一致しない場合 `403` を返す

同一 site 構成に寄せる場合だけ、次の案 A へ切り替えられる。

- frontend / backend を同一 site 配下に置く
- refresh cookie は `SameSite=Lax`
- CSRF は MVP では簡略化可能
- custom domain または reverse proxy を前提にする

Cookie 属性は環境変数または Spring profile で切り替える。

| 設定 | デフォルト | 説明 |
| --- | --- | --- |
| `AUTH_COOKIE_SAME_SITE` | `Lax` | 無料枠 cross-site 公開では `None` に変更する |
| `AUTH_COOKIE_SECURE` | `false` | 本番は `true`。`SameSite=None` の場合は必ず `true` |
| `AUTH_CSRF_ENABLED` | `false` | `SameSite=None` または cross-site cookie 運用では `true` 必須 |
| `FRONTEND_ORIGIN` | local dev URL | CORS の許可 origin。`*` は禁止 |

起動時 validation は未実装のため、本番設定前に追加する:

- `AUTH_COOKIE_SAME_SITE=None` かつ `AUTH_COOKIE_SECURE=false` の場合は起動エラーにする
- `AUTH_COOKIE_SAME_SITE=None` かつ `AUTH_CSRF_ENABLED=false` の場合は起動エラーにする
- production profile では `FRONTEND_ORIGIN` 未設定を起動エラーにする

## 10. Migration 方針

現行実装は `spring.jpa.hibernate.ddl-auto=update` を維持している。
起動時の `ReadStackDataInitializer` が初期 owner ユーザーを作成し、`user_id` が未設定の既存記事・タグへ owner を付与する。
本番公開前に `ddl-auto=update` 依存をやめ、Flyway などで migration を管理する。

段階:

1. Flyway などの DB migration 管理を導入する
2. `users` と `refresh_tokens` を追加する
3. 初期 owner ユーザーを作成する migration または one-off script を用意する
4. `articles.user_id` と `tags.user_id` を nullable で追加する
5. 既存データへ初期 owner を付与する
6. `articles` と `tags` に `UNIQUE (user_id, id)` を追加する
7. `article_tags.user_id` を nullable で追加し、既存の紐づけへ article owner を付与する
8. `article_tags.user_id` を NOT NULL に変更し、`PRIMARY KEY (user_id, article_id, tag_id)` と複合 FK を追加する
9. 既存の unique 制約を user scoped unique へ変更する
10. repository を user scoped query へ変更する
11. controller / service に current user を渡す
12. 認証 API を追加する
13. frontend auth を追加する
14. 最後に既存 API を認証必須化する
15. IT / E2E でユーザー分離を確認する

## 11. テスト観点

- 未ログインでは記事 API にアクセスできない
- 登録後に access token と refresh cookie が発行される
- login 後に自分の記事だけ取得できる
- ユーザー A の記事 ID をユーザー B が詳細取得しても見えない
- ユーザー A と B が同じ URL をそれぞれ登録できる
- ユーザー A と B が同じ tag name をそれぞれ登録できる
- ユーザー B がユーザー A の tagId を使って記事作成 / 更新しても紐づけできない
- application 層の検証漏れがあっても、`article_tags` の複合 FK が user mismatch を拒否する
- refresh token rotation 後、旧 token は使えない
- 失効済み refresh token の再利用時に、同一 `family_id` の未失効 token がすべて失効される
- expired refresh token の cookie が送られた場合、`401` と cookie 削除 `Set-Cookie` を返す
- logout 後、refresh できない
- `LOCKED` / `DELETED` user は refresh できない
- access token 期限の 60 秒前を目安に proactive refresh が実行される
- セッション切れ時に refresh 後リクエストが再送される
- refresh 失敗時にログイン画面へ戻る
- CORS origin が許可外の場合、cookie 認証 API が通らない
- 複数ユーザーの記事が増えた状態で、`user_id` を含む複合 index が一覧・検索・状態絞り込みで使われることを確認する

## 12. 未決事項

- JWT 署名を MVP から RS256 にするか
- access token の即時失効を `jti` blacklist で行うか、`users.token_valid_after` のような valid-after 方式で行うか
- refresh token cookie を同一 site 運用に寄せるため、初期から custom domain を用意するか
- メール確認、パスワードリセットを MVP 公開に含めるか
- rate limit をアプリ内で実装するか、デプロイ基盤や CDN に任せるか
- 退会済みユーザーのデータを論理削除のまま保持するか、一定期間後に記事・タグ・refresh token を物理削除するか
- `displayName` 変更用の `PATCH /api/users/me`、パスワード変更、パスワードリセットをどのタイミングで追加するか
- 全端末ログアウト用の `POST /api/auth/logout-all` を MVP に含めるか
