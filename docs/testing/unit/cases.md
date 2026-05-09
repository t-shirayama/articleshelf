# Unit Test Cases

## 1. UT ケース一覧

| ID | 優先度 | 対象 | 観点 | 期待結果 |
| --- | --- | --- | --- | --- |
| UT-BE-001 | P0 | Article | `status` 未指定 | `UNREAD` になる |
| UT-BE-002 | P0 | ArticleService.addArticle | URL 重複 | `DuplicateArticleUrlException` |
| UT-BE-003 | P0 | ArticleService.addArticle | OGP 不可 | 保存せず `ArticleUrlUnavailableException` |
| UT-BE-004 | P0 | ArticleService.updateArticle | URL 変更なし | 不要な OGP 再取得をしない |
| UT-BE-005 | P0 | ArticleService.deleteArticle | 存在しない ID | `ArticleNotFoundException` |
| UT-BE-006 | P1 | ArticleService.findArticles | `status`, `tag`, `search`, `favorite` の組み合わせ | 条件一致のみ返す |
| UT-BE-007 | P1 | Tag 正規化 | 空白、重複、空文字 | 空文字除外、重複統合 |
| UT-BE-008 | P1 | ErrorResponse | 重複 URL | `existingArticleId` を含む |
| UT-BE-009 | P1 | ApiExceptionHandler | 認証、タグ、パスワード、想定外例外 | 例外 reason に応じた文言と汎用 500 を返す |
| UT-BE-010 | P1 | ProductionEnvironmentValidator | 本番 CSRF / cookie 設定 | prod で CSRF 無効や `SameSite=None; Secure=false` を拒否する |
| UT-BE-011 | P1 | JwtTokenService | JWT 発行 / 検証 | HS256 token を発行し、改ざん、期限切れ、想定外 alg を拒否する |
| UT-BE-012 | P0 | UsernamePolicy | username 正規化 / 形式 | 3〜32文字、許可文字、小文字正規化を検証する |
| UT-BE-013 | P1 | AuthRateLimiter | 登録 / ログイン試行制限 | login は `IP + username`、register は IP 単位で超過時に拒否し、window 後に再許可する |
| UT-BE-014 | P0 | OgpRequestGuard | SSRF 対策 | scheme、localhost、loopback、private、link-local、multicast、metadata endpoint、IPv6 unique local を拒否する |
| UT-FE-001 | P0 | API adapter | Article response 変換 | UI が必要な型に変換される |
| UT-FE-002 | P0 | store | お気に入り楽観更新成功 | 一覧全体 reload なしで状態維持 |
| UT-FE-003 | P0 | store | お気に入り保存失敗 | 元状態へ戻しエラー表示 |
| UT-FE-004 | P0 | store | 既読化 | 既読日が操作日になる |
| UT-FE-005 | P0 | store | 未読化 | 既読日が未設定になる |
| UT-FE-006 | P1 | filter | 複数タグ | すべて一致か一部一致か仕様どおり |
| UT-FE-007 | P1 | filter | おすすめ度未設定 | `0` を条件として扱える |
| UT-FE-008 | P1 | sort | 登録日、更新日、既読日、タイトル、評価 | 安定した順序になる |
| UT-FE-009 | P1 | Markdown | raw HTML | 実行可能 HTML として描画しない |
| UT-FE-010 | P2 | title helper | フィルタ条件 | 一覧タイトルが条件に追従 |
| UT-FE-011 | P1 | API client / article actions | 4xx / 5xx / 通信失敗 / 401 retry / 一覧操作失敗 | 構造化エラーと表示先 error state へ変換する |
| UT-FE-012 | P0 | auth store | username 認証 / アカウント操作 | password change / logout-all / delete account 成功時に auth state をクリアする |

## 2. 実装済み UT

### バックエンド

- `ArticleTest`
  - status 未指定時に `UNREAD` になる
  - optional 文字列の初期値と rating clamp を確認する
- `PasswordPolicyTest`
  - 8〜128文字、username 同一不可の要件を確認する
- `UsernamePolicyTest`
  - username の小文字正規化、形式不正、長さ制約を確認する
- `ArticleServiceTest`
  - 記事追加時の OGP metadata 反映、タグ正規化、user scoped 保存
  - 同一ユーザー内の重複 URL 拒否と、別ユーザーの同一 URL 許可
  - アクセス不可 URL の保存拒否
  - status / tag / search / favorite の一覧絞り込み
  - 削除の user scope
- `ApiExceptionHandlerTest`
  - 重複 URL の `existingArticleId` 付きエラー応答
  - 認証、タグ、パスワードポリシー例外の reason code 別メッセージ
  - 想定外例外で内部メッセージを漏らさない汎用 500 応答
- `ProductionEnvironmentValidatorTest`
  - production profile で `AUTH_CSRF_ENABLED=false` を拒否する
  - `AUTH_COOKIE_SAME_SITE=None` と `AUTH_COOKIE_SECURE=false` の組み合わせを拒否する
  - secure cross-site production 設定を許可する
- `JwtTokenServiceTest`
  - Spring Security JOSE 経由で HS256 access token を発行 / 検証する
  - token 改ざん、期限切れ、`alg=none` のような想定外 header を拒否する

### フロントエンド

- `articleFilters.test.ts`
  - status / favorite / tag / rating / date range / search の複合絞り込み
  - rating 降順と updatedAt fallback の並び替え
- `articleForms.test.ts`
  - 「あとで読む」時の未読保存と既読日の null 化
  - 既読記事作成時の既読日保持
  - タグ正規化と詳細フォーム差分検知
- `articles.test.ts`
  - お気に入り楽観更新失敗時の rollback
  - ステータス楽観更新成功時の状態反映
  - ログアウト時の user scoped state 初期化
- `client.test.ts`
  - API `messages` と `existingArticleId` を `ApiRequestError` として保持する
  - 空の 401、通信失敗、5xx、malformed success response を汎用メッセージへ変換する
  - 401 後の refresh / retry で新しい access token を使う
- `useArticleActions.test.ts`
  - 一覧削除失敗や stale article 詳細取得失敗を一覧エラーバナー向け state へ変換する
