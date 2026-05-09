# セキュリティ仕様

最終更新: 2026-05-08

## 1. 位置づけ

セキュリティは一般に、性能、可用性、保守性と同じく非機能要件に含める。
ArticleShelf では、非機能仕様のうち具体的なセキュリティ対策をこの文書に切り出し、認証、入力、外部通信、表示、運用、テストの横断仕様として管理する。

セキュリティ対策を追加または更新した場合は、この文書を必ず更新する。
必要に応じて [authentication.md](authentication.md)、[non-functional.md](non-functional.md)、[../architecture/security.md](../architecture/security.md)、[../deployment/README.md](../deployment/README.md)、[../testing/README.md](../testing/README.md) も同期する。

## 2. 認証とトークン

- protected API は `Authorization: Bearer <accessToken>` を必須にする
- access token は短命 JWT とし、フロントエンドのメモリ上に保持する
- JWT の発行 / 検証、署名、`alg` header、`exp` 検証は自前実装せず、Spring Security JOSE の `NimbusJwtEncoder` / `NimbusJwtDecoder` に委譲する
- refresh token は HttpOnly cookie として扱い、DB には `HMAC-SHA256(AUTH_REFRESH_TOKEN_HASH_SECRET, raw_refresh_token)` の hash のみ保存する
- refresh token は rotation し、失効済み token の再利用時は同一 family の未失効 token をすべて失効する
- protected API では JWT の署名と期限だけでなく、ユーザーが `ACTIVE` であり token が `token_valid_after` より古くないことも確認する
- 認証、Cookie、CSRF、CORS の詳細仕様は [authentication.md](authentication.md) に従う

## 3. 本番起動ガード

- production profile では `AUTH_CSRF_ENABLED=true` を必須にする
- frontend と API が別 site の公開構成では `AUTH_COOKIE_SAME_SITE=None` と `AUTH_COOKIE_SECURE=true` を必須にする
- `FRONTEND_ORIGIN` は明示的な origin を指定し、CORS で `*` は使わない
- production profile では `JWT_ACCESS_SECRET` と `AUTH_REFRESH_TOKEN_HASH_SECRET` を必須にする
- 本番 secret は 32 文字以上にし、`dev-` 始まり、`change-me` を含む値は起動時に拒否する
- 通常起動では初期ユーザーを自動作成しない。検証環境で必要な場合のみ `ARTICLESHELF_INITIAL_USER_ENABLED=true` を明示する

## 4. ユーザースコープとデータ保護

- 記事、タグ、メモ、既読履歴は認証ユーザーの `user_id` で必ずスコープする
- 一覧、詳細、更新、削除はすべて `currentUser.id` を条件に含める
- 他ユーザーの記事 ID / タグ ID は存在しないものとして `404 Not Found` を返す
- application 層の検証に加え、DB の unique 制約や複合 FK で user mismatch を拒否する
- managed PostgreSQL を使う公開構成では JDBC URL 側で TLS を有効化する

## 5. 公開エンドポイントのレート制限

- `/api/auth/register` は Spring が確定した client IP 単位で制限する
- `/api/auth/login` は Spring が確定した client IP と正規化 username の組み合わせで制限する
- application code では `X-Forwarded-For` を直接読まず、forwarded header の解釈は Spring / servlet container 側に寄せる
- backend へ外部から直接到達させず、Render などの trusted proxy 経路からのみ到達する構成を前提にする
- 制限超過時は `429 Too Many Requests` と統一 JSON error body を返す
- Render 無料枠の単一インスタンス運用を前提に、backend in-memory の簡易制限とする
- 複数インスタンス構成では制限が分散するため、共有ストアまたは proxy 側 rate limit が必要になる

## 6. OGP 取得と SSRF 対策

ユーザー入力 URL に対して backend が直接 HTTP fetch するため、OGP 取得は SSRF 対策を必須にする。

- scheme は `http` / `https` のみ許可する
- URL userinfo は許可しない
- `localhost`、`localhost.localdomain` を拒否する
- DNS 解決後の全 IP を検証する
- loopback、private、link-local、multicast、any-local、IPv6 unique local を拒否する
- cloud metadata endpoint として `169.254.169.254` と `100.100.100.200` を明示拒否する
- redirect は自動追従せず、redirect 先 URL も同じ検証を行う
- redirect 回数は最大 3 回に制限する
- response body は最大 1MB に制限する
- `Content-Type` が `text/html` 以外の場合は本文を解析しない
- OGP 取得は timeout と専用 User-Agent を設定する

## 7. 入力検証とエラー応答

- バリデーションはフロントエンドとバックエンドの両方で実施する
- バックエンドは API 契約と永続化前の最終防衛線として `@NotBlank`、`@Size`、`@Min`、`@Max`、URL 検証などを行う
- 認証失敗、権限不足、他ユーザーデータ参照、入力不正、重複、想定外エラーは共通ルールで HTTP status と error body に変換する
- 想定外エラーでは内部実装の詳細をレスポンスに含めない

## 8. Markdown 表示

- 詳細画面のメモ Markdown はフロントエンドだけで HTML に変換し、バックエンドには元のメモ本文を保存する
- Markdown 変換では raw HTML を無効化する
- `v-html` に渡す HTML は必ず DOMPurify を通す
- `script`、`iframe`、`object`、`embed`、`style`、フォーム系タグ、SVG、MathML、media 系タグは禁止する
- リンクは `http` / `https` / `mailto` のみ許可する
- 画像は `http` / `https` のみ許可し、`data:` や `javascript:` などのスキームは表示しない
- コードブロックは静的に装飾するだけで、コード本文を評価・実行しない

## 9. テスト観点

- production profile で CSRF 無効、`SameSite=None; Secure=false`、未設定または弱い secret を拒否する
- JWT は Spring Security JOSE 経由で発行 / 検証し、改ざん、期限切れ、想定外 `alg` を拒否する
- refresh token は hash 保存、rotation、reuse detection、全端末失効を検証する
- register / login の rate limit は制限 key、429 応答、統一 error body を検証する
- OGP SSRF 対策は scheme、localhost、loopback、private、link-local、multicast、metadata endpoint、IPv6 unique local、redirect 先再検証、body size、content-type を検証する
- Markdown sanitization は raw HTML、危険タグ、危険属性、危険 scheme が実行または表示されないことを検証する
