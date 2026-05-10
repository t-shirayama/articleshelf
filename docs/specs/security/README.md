# セキュリティ仕様

最終更新: 2026-05-08

## 1. 位置づけ

セキュリティ要件は [../../requirements/non-functional/security.md](../../requirements/non-functional/security.md) を正とする。
この文書では、現在の実装における具体的なセキュリティ仕様を、認証、入力、外部通信、表示、運用、テストの横断仕様として管理する。
脆弱性疑いの private reporting flow、supported scope、報告時に含めてほしい情報は repository root の [SECURITY.md](../../../SECURITY.md) を入口とする。

セキュリティ対策を追加または更新した場合は、この文書を必ず更新する。
必要に応じて [認証仕様](../auth/README.md)、[品質仕様](../quality/README.md)、[../../requirements/non-functional/security.md](../../requirements/non-functional/security.md)、[../../deployment/README.md](../../deployment/README.md)、[../../testing/README.md](../../testing/README.md) も同期する。

## 2. 認証、トークン、CSRF

認証 ID、protected API、access token、refresh token、Cookie、CSRF の詳細仕様は [認証仕様](../auth/README.md) と [Token / Cookie / CSRF](../auth/tokens.md) を正本とする。
この文書では、セキュリティ横断で同期が必要な境界だけを扱う。

- protected API は認証済みユーザーだけが利用できる
- access token は短命 JWT、refresh token は HttpOnly cookie として扱う
- Cookie 認証を使う refresh / logout 系 API は CSRF 保護の対象にする
- refresh token rotation は `RefreshTokenRotationService` に集約し、pessimistic lock と条件付き update で atomic に行い、並行 refresh で複数 replacement token が有効化されないようにする
- JWT の発行 / 検証は Spring Security JOSE に委譲し、自前実装しない

## 3. 本番起動ガード

- production profile では `AUTH_CSRF_ENABLED=true` を必須にする
- frontend と API が別 site の公開構成では `AUTH_COOKIE_SAME_SITE=None` と `AUTH_COOKIE_SECURE=true` を必須にする
- `FRONTEND_ORIGIN` は明示的な origin を指定し、CORS で `*` は使わない
- production profile では `JWT_ACCESS_SECRET` と `AUTH_REFRESH_TOKEN_HASH_SECRET` を必須にする
- 本番 secret は 32 文字以上にし、`dev-` 始まり、`change-me` を含む値は起動時に拒否する
- production profile では `SPRING_DATASOURCE_URL` に `sslmode=require`、`verify-ca`、`verify-full` のいずれかを必須にする
- backend の production Docker final image は dedicated non-root user で実行する
- frontend の Docker 開発 / E2E image も Node 公式 image の non-root user で実行し、Trivy の container escape misconfiguration を避ける
- 通常起動では初期ユーザーを自動作成しない。検証環境で必要な場合のみ `ARTICLESHELF_INITIAL_USER_ENABLED=true` を明示する

## 4. ユーザースコープとデータ保護

- 記事、タグ、メモ、既読履歴は認証ユーザーの `user_id` で必ずスコープする
- 他ユーザーの記事 ID / タグ ID は存在しないものとして扱う
- application 層の検証に加え、DB の unique 制約や複合 FK で user mismatch を拒否する
- managed PostgreSQL を使う公開構成では JDBC URL 側で TLS を有効化し、production profile の起動ガードで `sslmode=require` 以上を検証する

ユーザー単位の所有データと制約は [データモデル](../data/README.md) を正本とする。

## 5. 公開エンドポイントのレート制限

登録 / ログインの rate limit 対象、制限 key、既定値、429 応答は [アカウント API](../auth/account-api.md) を正本とする。
runtime 上の単一インスタンス前提と複数インスタンス移行時の考慮は [Runtime Architecture](../../architecture/runtime/README.md) に従う。

- application code では `X-Forwarded-For` を直接読まず、forwarded header の解釈は Spring / servlet container 側に寄せる
- application code の client IP 解決は `ClientIpResolver` に集約し、現行は Spring / servlet container が確定した `remoteAddr` を使う
- backend へ外部から直接到達させず、Render などの trusted proxy 経路からのみ到達する構成を前提にする
- access token の invalid / expired / inactive user などの拒否は token 値を記録せず、`articleshelf.auth.access_token_rejected` の reason のみで観測する

## 6. OGP 取得と SSRF 対策

ユーザー入力 URL に対して backend が直接 HTTP fetch するため、OGP 取得は SSRF 対策を必須にする。

- scheme は `http` / `https` のみ許可する
- URL userinfo は許可しない
- `localhost`、`localhost.localdomain` を拒否する
- DNS 解決後の全 IP を検証する
- 接続直前にも DNS を再解決し、その時点の全 IP を検証する。連続した解決結果の完全一致は要求せず、危険 IP が混ざる場合は DNS rebinding / TOCTOU リスクとして拒否する
- loopback、private、link-local、multicast、any-local、IPv6 unique local を拒否する
- cloud metadata endpoint として `169.254.169.254` と `100.100.100.200` を明示拒否する
- redirect は自動追従せず、redirect 先 URL も同じ検証を行う
- redirect 先 URL でも scheme、userinfo、host、DNS、接続直前再解決を同じ手順で検証する
- redirect 回数は最大 3 回に制限する
- response body は最大 1MB に制限する
- `Content-Type` が `text/html` 以外の場合は本文を解析しない
- HTML は最大 1MB の bytes を読んだ後、`Content-Type` charset、meta charset、UTF-8 fallback の順で decode する
- OGP 取得は timeout と専用 User-Agent を設定する
- OGP 取得は DB transaction 外で同期実行し、外部サイトの遅延や失敗を保存 transaction に持ち込まない

## 7. 入力検証とエラー応答

- バリデーションはフロントエンドとバックエンドの両方で実施する
- バックエンドは API 契約と永続化前の最終防衛線として `@NotBlank`、`@Size`、`@Min`、`@Max`、URL 検証などを行う
- 認証失敗、権限不足、他ユーザーデータ参照、入力不正、重複、想定外エラーは共通ルールで HTTP status と error body に変換する
- 想定外エラーでは内部実装の詳細をレスポンスに含めない

## 8. Markdown 表示の安全境界

- 詳細画面のメモ Markdown はフロントエンドだけで HTML に変換し、バックエンドには元のメモ本文を保存する
- Markdown 変換と sanitization は `frontend/src/features/articles/domain/renderMarkdown.ts` が担当する
- Markdown 変換では raw HTML を無効化する
- `v-html` に渡す HTML は必ず DOMPurify を通す
- `script`、`iframe`、`object`、`embed`、`style`、フォーム系タグ、SVG、MathML、media 系タグは禁止する
- リンクは `http` / `https` / `mailto` のみ許可し、外部リンクには `target="_blank"` と `rel="noopener noreferrer nofollow"` を付ける
- 画像は `http` / `https` のみ許可し、`data:` や `javascript:` などのスキームは表示しない
- コードブロックは静的に装飾するだけで、コード本文を評価・実行しない

## 9. Frontend security headers

Cloudflare Pages の `_headers` で frontend response に security headers を付与する。

- CSP は `script-src 'self'`、`object-src 'none'`、`base-uri 'none'`、`frame-ancestors 'none'` を含める
- Vuetify の runtime style と既存 CSS 運用のため、`style-src` は当面 `'unsafe-inline'` を許可する
- Markdown / OGP 画像表示のため `img-src` は `https:`、`data:`、`blob:` を許可する。ただし Markdown renderer 側は画像 URL を `http` / `https` に制限する
- API 通信のため `connect-src` は `https:` と local development の `http://localhost:8080` を許可する
- `X-Content-Type-Options: nosniff`、`Referrer-Policy`、`Strict-Transport-Security`、`Permissions-Policy` を付与する

## 10. テスト観点

- production profile で CSRF 無効、`SameSite=None; Secure=false`、未設定または弱い secret、TLS 無効の datasource URL を拒否する
- JWT は Spring Security JOSE 経由で発行 / 検証し、改ざん、期限切れ、想定外 `alg` を拒否する
- refresh token は hash 保存、rotation、reuse detection、全端末失効を検証する
- register / login の rate limit は制限 key、429 応答、統一 error body を検証する
- OGP SSRF 対策は scheme、localhost、loopback、private、link-local、multicast、metadata endpoint、IPv6 unique local、redirect 先再検証、body size、content-type を検証する
- Markdown sanitization は raw HTML、危険タグ、危険属性、危険 scheme が実行または表示されないことを検証する
- Frontend security headers は Cloudflare Pages `_headers` を正本とし、CSP / nosniff / Referrer-Policy / HSTS / Permissions-Policy の存在をレビューで確認する
- Supply chain security は Dependabot、Dependency Review、CodeQL、Trivy で分担する。Dependency Review は Dependency graph と repository variable `DEPENDENCY_REVIEW_ENABLED=true` が有効な repository で PR の依存差分を確認し、未対応 repository では skip notice に留める。CodeQL は Java / JavaScript / TypeScript の静的解析、Trivy は filesystem と backend Docker image の high / critical vulnerability、secret、misconfiguration を確認する。Trivy で固定版が示された dependency vulnerability は patch version を優先して更新する
