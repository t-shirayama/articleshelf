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
- 記事、タグ、記事タグは必ず `user_id` に紐づける
- API の検索・詳細・更新・削除はすべて認証ユーザーの `user_id` でスコープする
- ユーザー間で同じ URL や同じタグ名を登録できる
- DB migration を導入し、既存データの owner 付与を明示的に行う

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
  - `roles`: `USER`
  - `iat`: 発行時刻
  - `exp`: 失効時刻
  - `jti`: token id

### 4.2 Refresh Token

- 形式: ランダムな不透明 token を推奨
- 保存: DB には hash 化して保存する
- 有効期限: 30 日
- 送信方法: HttpOnly cookie
- Cookie 属性:
  - `HttpOnly`
  - `Secure`
  - `SameSite=Lax` を基本とする
  - フロントエンドと API が異なる site になる無料枠構成では `SameSite=None; Secure` が必要になるため、refresh / logout には CSRF 対策を追加する
- rotation:
  - refresh 成功時に新しい refresh token を発行する
  - 旧 refresh token は即時失効する
  - 失効済み refresh token が再利用された場合は、同一 user の refresh token family を全失効する

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
| 追加 | `user_id UUID NOT NULL` |
| unique | `url` 単独 unique を廃止し、`(user_id, url)` unique に変更 |
| index | `(user_id, status)`, `(user_id, favorite)`, `(user_id, created_at)` |

### 5.4 tags 変更

| 変更 | 内容 |
| --- | --- |
| 追加 | `user_id UUID NOT NULL` |
| unique | `name` 単独 unique ではなく `(user_id, lower(name))` 相当 |
| index | `(user_id, name)` |

### 5.5 article_tags

記事とタグが同一ユーザーに属することを application 層で保証する。
DB では `article_id`, `tag_id` の FK に加え、可能であれば複合 FK または制約で user mismatch を防ぐ。

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
  - `accessToken`
- 副作用:
  - refresh token を rotation し、新 cookie を設定
- エラー:
  - `401`: refresh token 不正、期限切れ、失効済み

#### `POST /api/auth/logout`

- 説明: ログアウト
- 認証: refresh token cookie または access token
- レスポンス: `204 No Content`
- 副作用:
  - refresh token を失効
  - cookie を削除

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
  - `role`

## 7. バックエンド設計

### 7.1 依存追加

`backend/pom.xml` に追加する候補:

- `spring-boot-starter-security`
- JWT ライブラリ
  - `io.jsonwebtoken:jjwt-*` または `com.auth0:java-jwt`
- `spring-boot-starter-test`
- `spring-security-test`
- migration 導入時は `flyway-core`

### 7.2 パッケージ構成

- `com.readstack.domain.user`
  - `User`
  - `UserRepository`
  - `RefreshToken`
  - `RefreshTokenRepository`
  - `PasswordPolicy`
- `com.readstack.application.auth`
  - `AuthService`
  - `RegisterCommand`
  - `LoginCommand`
  - `TokenPair`
  - `CurrentUser`
- `com.readstack.infrastructure.security`
  - `JwtTokenService`
  - `PasswordEncoderConfig`
  - `SecurityConfig`
  - `JwtAuthenticationFilter`
- `com.readstack.infrastructure.persistence`
  - `UserEntity`
  - `RefreshTokenEntity`
  - `SpringDataUserJpaRepository`
  - `JpaUserRepository`
- `com.readstack.adapter.web`
  - `AuthController`
  - `UserController`

### 7.3 Spring Security

- `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/actuator/health` は認証不要
- `/api/**` は認証必須
- CSRF:
  - Bearer token API は CSRF 無効
  - cookie を使う refresh / logout は SameSite 設計に応じて CSRF token を検討
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

## 8. フロントエンド設計

### 8.1 画面

- 登録画面
- ログイン画面
- ログアウト導線
- ログイン後の ReadStack workspace
- 未ログイン時の保護 route redirect

### 8.2 状態管理

`features/auth` を追加する。

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
  - `LoginForm`
  - `RegisterForm`
  - `AuthLayout`

Access token は `localStorage` に保存せず、Pinia store のメモリ上に保持する。
画面 reload 時は `/api/auth/refresh` を呼び、refresh cookie が有効なら access token を再取得する。

### 8.3 API client

- request interceptor 相当で `Authorization: Bearer` を付与する
- `401` を受けたら一度だけ refresh を試す
- refresh 成功後に元リクエストを再送する
- refresh 失敗時は auth store をクリアし、ログイン画面へ遷移する
- 複数リクエストが同時に `401` になった場合は refresh を 1 回にまとめる

### 8.4 UX

- 登録後は空の一覧画面へ遷移し、記事追加を促す
- ログイン失敗時はフォーム内にエラーを表示し、入力内容は保持する
- ログアウト後は access token と記事 store を破棄する
- ユーザー切り替え時に前ユーザーの記事一覧が一瞬表示されないよう、auth 変更時に article store を初期化する
- セッション切れ時は操作中のフォームを失いにくいよう、保存直前の `401` では refresh 後に再送する

## 9. セキュリティ要件

### 9.1 パスワード

- 8 文字以上
- メールアドレスと同一不可
- BCrypt など adaptive hash を使う
- 平文 password をログに出さない
- 登録・ログイン API は rate limit の導入を検討する

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
その場合は次を満たす。

- HTTPS 必須
- refresh cookie は `Secure`
- cross-site cookie が必要なら `SameSite=None`
- `Access-Control-Allow-Credentials` を有効化
- `Access-Control-Allow-Origin` は `*` にしない
- refresh / logout には CSRF token または同等の対策を入れる

## 10. Migration 方針

本番公開前に `spring.jpa.hibernate.ddl-auto=update` 依存をやめ、Flyway などで migration を管理する。

段階:

1. `users` と `refresh_tokens` を追加
2. 初期 owner ユーザーを作成する migration または one-off script を用意
3. `articles.user_id` と `tags.user_id` を nullable で追加
4. 既存データへ初期 owner を付与
5. `user_id` を NOT NULL に変更
6. 既存の unique 制約を user scoped unique へ変更
7. application / API を user scoped query へ変更
8. 認証必須化
9. IT / E2E でユーザー分離を確認

## 11. テスト観点

- 未ログインでは記事 API にアクセスできない
- 登録後に access token と refresh cookie が発行される
- login 後に自分の記事だけ取得できる
- ユーザー A の記事 ID をユーザー B が詳細取得しても見えない
- ユーザー A と B が同じ URL をそれぞれ登録できる
- refresh token rotation 後、旧 token は使えない
- logout 後、refresh できない
- セッション切れ時に refresh 後リクエストが再送される
- refresh 失敗時にログイン画面へ戻る

## 12. 未決事項

- JWT 署名を MVP から RS256 にするか
- refresh token cookie を同一 site 運用に寄せるため、初期から custom domain を用意するか
- メール確認、パスワードリセットを MVP 公開に含めるか
- rate limit をアプリ内で実装するか、デプロイ基盤や CDN に任せるか
