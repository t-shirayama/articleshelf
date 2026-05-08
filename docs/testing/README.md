# ReadStack Test Strategy

最終更新: 2026-05-08

## 1. 目的

ReadStack のテストは、記事を「読んだ知識資産」として安全に登録・検索・更新できることを継続的に保証するために整備する。
本ドキュメントでは、UT（Unit Test）、IT（Integration Test）、E2E（End-to-End Test）の目的、スコープ、観点、ケース、実行方針、CI/CD 連携を定義する。

経営・プロダクト観点では、公開前に次の品質リスクを下げることを目的にする。

- ユーザーが保存した記事、メモ、タグ、既読履歴を失わない
- 重要な導線である記事追加、一覧検索、詳細編集、削除、カレンダー確認が壊れていない
- 認証追加後に、他ユーザーのデータが見えない
- UI 改修やレスポンシブ対応後も、主要な操作が継続して使える
- CI/CD により、公開環境へ壊れた変更が入る可能性を下げる

## 2. テスト方針

### 2.1 テストピラミッド

ReadStack では、実行速度と保守性を重視し、次の比率を目安に整備する。

| 種別 | 主目的 | 件数の目安 | 実行タイミング |
| --- | --- | --- | --- |
| UT | 関数、ドメイン、ユースケース単位の仕様を高速に検証 | 多め | ローカル、PR、push |
| IT | API、DB、Spring 設定、永続化、認証境界を検証 | 中程度 | PR、main push |
| E2E | ブラウザ上の主要ユーザーシナリオを検証 | 少数精鋭 | PR、main push、リリース前 |

E2E は便利だが壊れやすく遅くなりやすい。細かい分岐は UT / IT へ寄せ、E2E は役員説明やリリース判定で「主要導線が動く」と説明できる代表シナリオに絞る。

### 2.2 優先度

| 優先度 | 意味 | 対象例 |
| --- | --- | --- |
| P0 | 公開前に必須。壊れるとデータ消失、情報漏えい、主要導線停止につながる | 記事追加、保存、編集、削除、認証、ユーザースコープ、CI |
| P1 | MVP の信頼性に必要。壊れると体験品質が大きく落ちる | 検索、フィルタ、タグ、カレンダー、Markdown 表示 |
| P2 | 使い勝手や将来拡張の品質を上げる | 細かな表示状態、境界値、アクセシビリティ補助 |

### 2.3 現在の前提

- フロントエンド: Vue 3 + TypeScript + Pinia + Vuetify
- フロントエンド確認: `npm run build`
- フロントエンド UT: `npm run test:unit`
- フロントエンド UT coverage: `npm run test:unit:coverage`
- ブラウザ E2E: `npm run test:e2e`
- バックエンド: Java 21 + Spring Boot + Spring Data JPA
- DB: PostgreSQL
- バックエンド確認: ローカル `mvn` ではなく Docker 経由で `docker compose run --rm backend mvn test` を実行する
- バックエンド UT coverage: `docker compose run --rm backend mvn -Pcoverage test -Dtest='ArticleTest,PasswordPolicyTest,UsernamePolicyTest,ArticleServiceTest,ApiExceptionHandlerTest,JwtTokenServiceTest,ProductionEnvironmentValidatorTest'`
- 既存 CI: `.github/workflows/ci.yml` でフロントエンド UT / build、バックエンド UT / IT、E2E を実行する

## 3. UT: Unit Test

### 3.1 目的

UT は、外部 I/O に依存しない小さな単位で仕様を固定する。
主にドメインルール、変換処理、フィルタ・ソート、フォーム状態、API 入出力変換を対象にし、変更時にすぐ壊れた箇所を特定できる状態を作る。

### 3.2 スコープ

#### バックエンド

- `domain`
  - `Article` の初期値、ステータス、日付、評価値、タグ正規化
  - `ArticleStatus` の扱い
  - ドメイン例外の発生条件
- `application`
  - `ArticleService` の記事追加、更新、削除、検索条件
  - OGP 取得結果の適用
  - 重複 URL 検出
  - アクセス不可 URL の拒否
  - 未読 / 既読切り替え時の既読日ルール
- `adapter`
  - request DTO から command への変換
  - error response の組み立て
- `infrastructure`
  - OGP HTML 解析を分離した場合の parser
  - JPA entity と domain の mapping は純粋関数化できる範囲を UT 化

#### フロントエンド

- `features/articles/domain`
  - 検索、フィルタ、ソート、日付範囲判定
  - フォーム値から API input への変換
  - API response から表示モデルへの変換
- Pinia store
  - 記事一覧取得後の状態更新
  - 楽観的更新と失敗時 rollback
  - フィルタ条件、タイトル表示、カレンダー表示モード
- Markdown 表示 helper
  - raw HTML 無効化
  - DOMPurify に渡す前後の変換境界
  - 許可スキームの判定
- UI component
  - 重要な分岐表示のみ。詳細な見た目は E2E / visual capture へ寄せる

### 3.3 対象外

- 実 DB への接続
- 実ブラウザ操作
- 外部サイトへの OGP 取得
- CSS のピクセル完全一致
- GitHub Actions や Render など外部サービスの実通信

### 3.4 推奨ツール

| 領域 | 推奨 |
| --- | --- |
| バックエンド | JUnit 5, Mockito または Spring Boot 標準 test starter |
| フロントエンド | Vitest, Vue Test Utils, Pinia testing helper |
| TypeScript 型 | `vue-tsc --noEmit` |

現行実装では `frontend/package.json` に `vitest` と `jsdom` を追加し、`npm run test:unit` で実行する。
coverage 確認は `npm run test:unit:coverage` で実行し、text summary と `frontend/coverage/` の HTML / lcov report を確認する。
バックエンドは `spring-boot-starter-test`, `spring-security-test`, `h2` を追加し、`docker compose run --rm backend mvn test` で実行する。
Unit coverage は Maven の `coverage` profile で JaCoCo を有効にし、`backend/target/site/jacoco/` に report を出力する。CI では JaCoCo CSV から domain / application 層の line coverage を集計し、80% 未満なら失敗させる。長期目標は 100% とし、未カバーの分岐や例外系は段階的にテストを追加する。

### 3.5 UT ケース一覧

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
| IT-BE-006 | P1 | Auth rate limit API | 429 応答 | `X-Forwarded-For` の IP を使い、register / login の超過時に統一 JSON エラーを返す |
| IT-API-010 | P1 | ArticleRequest validation | API 境界の入力制約 | `rating` 範囲外や長すぎる `title` を 400 で拒否する |
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

### 3.6 実装済み UT

#### バックエンド

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

#### フロントエンド

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

## 4. IT: Integration Test

### 4.1 目的

IT は、複数レイヤーを結合したときの契約を検証する。
ReadStack では API、DB、JPA mapping、Spring 設定、CORS、認証、トランザクション、エラーレスポンスを中心に確認する。

### 4.2 スコープ

- `@SpringBootTest` または `@WebMvcTest` による API 検証
- PostgreSQL と JPA の結合
- リポジトリの検索、保存、削除
- API の request / response JSON
- バリデーションと例外ハンドリング
- CORS 設定
- 認証追加後の security filter chain
- ユーザーごとのデータ分離
- `/actuator/health` など公開時の health check

### 4.3 対象外

- 実外部サイトへの OGP 取得
- 実ブラウザの UI 操作
- Render / GitHub Actions など外部環境そのものの稼働保証
- 画面デザインの見た目

### 4.4 推奨ツール

| 領域 | 推奨 |
| --- | --- |
| API | Spring Boot Test, MockMvc |
| DB | Testcontainers PostgreSQL または Docker Compose 上の PostgreSQL |
| 認証 | Spring Security Test |
| OGP | WireMock などの stub server、または `ArticleMetadataProvider` の test double |

公開前は、本番に近い PostgreSQL で検証できるよう Testcontainers の採用を推奨する。
ただし Docker in Docker の CI 負荷が高い場合は、まず `docker compose run --rm backend mvn test` のように backend コンテナからテストを実行する方式に寄せる。
現行 IT は H2 の PostgreSQL mode を使い、Spring Security / MockMvc / JPA / API 契約を軽量に検証する。
Spring Data JPA の `@Query`、JPQL、native SQL、Repository の検索条件、Flyway migration、DB 制約を変更した場合は、H2 だけでなく PostgreSQL 実体を使う persistence IT を実行する。
特に `LIKE`、`concat`、`coalesce`、nullable parameter、enum、UUID、日付、JOIN を含む条件は DB 方言や型推論の差が出やすいため、該当条件を `JpaArticleRepositoryPostgresIntegrationTest` などに追加してから `docker compose run --rm backend mvn -Dtest=... test` で確認する。

### 4.5 IT ケース一覧

| ID | 優先度 | 対象 | 観点 | 期待結果 |
| --- | --- | --- | --- | --- |
| IT-API-001 | P0 | `POST /api/articles` | 正常登録 | `200` または `201`、保存値が返る |
| IT-API-002 | P0 | `POST /api/articles` | URL 未入力 | `400`、`messages` を返す |
| IT-API-003 | P0 | `POST /api/articles` | 重複 URL | `409`、`existingArticleId` を返す |
| IT-API-004 | P0 | `GET /api/articles/{id}` | 存在しない ID | `404` |
| IT-API-005 | P0 | `PUT /api/articles/{id}` | タグ置換 | 古い関連が消え、新しい関連になる |
| IT-API-006 | P0 | `DELETE /api/articles/{id}` | 削除 | 以降の詳細取得が `404` |
| IT-API-007 | P1 | `GET /api/articles` | 検索 | タイトル、URL、概要、メモに一致 |
| IT-API-008 | P1 | `GET /api/articles` | タグ単一条件 | 大文字小文字を区別せず一致 |
| IT-API-009 | P1 | `GET /api/tags` | タグ一覧 | 名前昇順 |
| IT-DB-001 | P0 | ArticleEntity | `Article` 保存 | UUID、日付、enum、tag 関連が保持される |
| IT-DB-002 | P0 | unique URL | DB 制約 | 同一 URL が二重保存されない |
| IT-DB-003 | P1 | Repository 検索 | PostgreSQL 実体で `LIKE`、nullable parameter、JOIN を含む複合条件 | SQL 型推論エラーを起こさず条件一致のみ返す |
| IT-AUTH-001 | P0 | 認証追加後 | 未ログイン | 保護 API は `401` |
| IT-AUTH-002 | P0 | 認証追加後 | ユーザー A の token でユーザー B の記事 ID | `404` または `403` |
| IT-AUTH-003 | P0 | 認証追加後 | refresh token rotation | 旧 refresh token は再利用不可 |
| IT-OPS-001 | P0 | health check | DB 接続正常 | `2xx` |
| IT-OPS-002 | P1 | CORS | 許可 origin | 認証 cookie / header を送れる |

### 4.6 実装済み IT

- `AuthAndArticleIntegrationTest`
  - 未認証の保護 API が `401` を返す
  - 登録ユーザーが記事を作成できる
  - ユーザー B の一覧・詳細からユーザー A の記事が見えない
  - URL 重複判定が user scoped で動く
  - malformed JSON、UUID / enum / boolean 型不正を `400` の統一エラー形式で返す
  - タグ名重複、同一タグ統合、存在しないタグ、使用中タグ削除のエラー応答
  - refresh token rotation 後、旧 refresh token の再利用が `401` になる

## 5. E2E: End-to-End Test

### 5.1 目的

E2E は、ユーザーがブラウザで使う主要導線を、フロントエンド、バックエンド、DB を結合して検証する。
経営説明では「公開前の代表操作は自動で担保している」と示せるテスト群にする。

### 5.2 スコープ

- Vite frontend
- Spring Boot backend
- PostgreSQL
- Playwright による Chromium 実行
- デスクトップ幅の代表確認
- スマホ幅の代表確認はスマホ UI 実装後に mobile project として追加する
- username 登録、ログイン、ログアウト、アカウント操作、ユーザースコープ

### 5.3 対象外

- 外部サイトの OGP 安定性
- アプリ外運用で行う管理者リセット後の本人通知
- 本番インフラ障害
- すべてのブラウザ組み合わせ
- CSS の完全一致

### 5.4 E2E シナリオ

| ID | 優先度 | シナリオ | 手順概要 | 期待結果 |
| --- | --- | --- | --- | --- |
| E2E-001 | P0 | 記事追加 | URL 入力、保存 | 一覧に記事が表示される |
| E2E-002 | P0 | 重複 URL | 同じ URL を追加 | エラーと既存詳細導線が表示される |
| E2E-003 | P0 | 詳細編集 | 詳細へ移動、メモ・タグ・評価を編集、保存 | 一覧と詳細に更新値が反映 |
| E2E-004 | P0 | 未読 / 既読切り替え | カードで既読化、元に戻す | ステータスと既読日が正しく変わる |
| E2E-005 | P0 | 削除 | カードまたは詳細から削除 | 一覧から消える |
| E2E-006 | P1 | 検索・フィルタ | 検索、タグ、評価、日付範囲を適用 | 条件に合う記事だけ表示 |
| E2E-007 | P1 | カレンダー | 登録日 / 既読日モード切替、日付の記事を開く | 対象記事の詳細へ遷移 |
| E2E-008 | P1 | Markdown 表示 | メモにコード、表、リンクを入力 | 安全に整形表示される |
| E2E-009 | P0 | ユーザー登録 | username・パスワード登録 | ログイン状態で空の一覧へ遷移 |
| E2E-010 | P0 | ログイン / ログアウト | ログイン、記事追加、ログアウト | ログアウト後は保護画面に入れない |
| E2E-011 | P0 | ユーザー分離 | ユーザー A と B で記事登録 | 互いの記事が一覧に出ない |
| E2E-012 | P0 | パスワード変更 | 現在 password と新 password を入力 | 旧 password でログイン不可、新 password でログイン可 |
| E2E-013 | P0 | 全端末ログアウト | アカウント設定から全端末ログアウト | ログイン画面へ戻る |
| E2E-014 | P0 | 退会 | 現在 password を入力して退会 | 同じ username でログインできない |
| E2E-015 | P0 | 管理者リセット | ADMIN token で password reset | 新 password でログインできる |
| E2E-016 | P1 | スマホ一覧 | 375px 幅で一覧、メニュー、追加 | ハンバーガー、ボトムナビ、全画面モーダルが使える |
| E2E-017 | P1 | スマホ詳細 | 375px 幅で詳細編集 | 右カラム情報が縦積みで操作できる |

### 5.5 テストデータ

- E2E ではテストごとに一意 username / URL を使い、既存データやサンプル seed に依存しない
- IT の OGP 取得は application port を test double で差し替える
- E2E の URL は `https://example.com/?readstackE2e=...` のようにケースごとに一意にする
- 認証ユーザーはケースごとに一意な username を使う
- 通常起動や build ではサンプルデータを自動投入しない
- 任意実行の `npm run seed:sample` は手動確認用であり、UT / IT / E2E はこのコマンドに依存しない
- 管理者リセット E2E だけ `docker-compose.e2e.yml` の `READSTACK_INITIAL_USER_ENABLED=true` で初期 ADMIN を明示作成する

### 5.6 Playwright 実行方針

推奨 npm script:

```json
{
  "scripts": {
    "test:e2e": "playwright test",
    "test:e2e:ui": "playwright test --ui"
  }
}
```

現行 `frontend/package.json` には次の script を追加済み。

```json
{
  "scripts": {
    "test:unit": "vitest run",
    "test:e2e": "playwright test",
    "test:e2e:ui": "playwright test --ui"
  }
}
```

推奨構成:

- `frontend/e2e/` に spec を配置
- `playwright.config.ts` で desktop project を定義
- スマホ UI 実装後に mobile project を追加する
- `webServer` は `docker compose up --build` を前面起動し、frontend / backend / PostgreSQL をまとめて起動する
- Compose は backend の `/actuator/health` healthcheck が healthy になってから frontend を起動し、登録直後の API 接続失敗を避ける
- CI では既存サーバーを再利用せず、Playwright が起動した Docker Compose プロセスを監視する
- ローカルでは既に `docker compose up --build` 済みのサーバーがあれば再利用する
- スクリーンショット比較は初期段階では必須にせず、主要表示が非空で操作可能なことを優先する

### 5.7 実装済み E2E

- `authenticated-articles.spec.ts`
  - ユーザー登録、記事追加、ログアウト、再ログイン後の記事表示
  - 重複 URL エラーと登録済み記事詳細への導線
  - 詳細画面でのメモ、タグ、おすすめ度、概要、タイトル編集
  - 詳細画面からの記事削除
  - 一覧カードからの未読 / 既読切り替え
  - 一覧カードからのお気に入り追加とお気に入り一覧フィルタ
  - 検索、タグ、おすすめ度による複合フィルタ
  - タイトル順ソート
  - カレンダーの登録日 / 既読日モードからの記事詳細遷移
  - Markdown メモのリンク / code 表示と script 要素の非描画
  - 未保存編集がある状態の画面遷移警告
  - タグ管理画面でのタグ検索、タグ記事一覧遷移、単独タグ作成、未使用タグ削除、タグ名変更、タグ統合
  - 日本語 / English 切り替えと選択保持
  - ユーザー A と B の記事一覧分離
  - ユーザー A の記事に対するユーザー B の API update / delete 拒否

現行 E2E は desktop Chromium で実行する。
OGP 取得の安定性に依存しすぎないよう、記事追加 URL は `https://example.com/?readstackE2e=...` の一意 URL を使う。
スマホ向け E2E は、スマホ UI 実装後に mobile project を追加する。

## 6. CI/CD 連携

### 6.1 現在の CI

現在の `.github/workflows/ci.yml` は次を実行する。

- Step 1: `backend-check` / `frontend-check`
  - backend は `docker compose run --rm backend mvn clean compile spotbugs:check` と `docker compose run --rm backend mvn test -Dtest=CleanArchitectureDependencyTest` でコンパイル、SpotBugs、依存方向を確認する
  - frontend は `npm run typecheck` と `npm run build` で型チェックと Vite ビルドを確認する
- Step 2: `backend-unit` / `frontend-unit`
  - backend の domain / application UT と frontend の Vitest UT を coverage 付きで実行する
  - backend は JaCoCo CSV から instruction / branch / line coverage summary を出力し、domain / application line coverage 80% 未満を失敗にする
  - frontend は Vitest coverage-v8 の text summary を出力する
- Step 3: `backend-integration` / `frontend-integration`
  - backend の Spring Boot / PostgreSQL IT と frontend の `*.integration.test.ts` を分けて実行する
- Step 4: `e2e`
  - Playwright Chromium で P0 導線を確認する

`main` / `develop` では全ジョブを実行し、それ以外のブランチでは変更パスに応じて backend / frontend / E2E の関連ジョブだけを実行する。

### 6.2 段階的な拡張

| 段階 | 追加内容 | 目的 |
| --- | --- | --- |
| Phase 1 | backend に `spring-boot-starter-test` を追加し、Docker 経由の backend test を実行 | 実装済み |
| Phase 2 | frontend に Vitest を追加し `npm run test:unit` を実行 | 実装済み |
| Phase 3 | Playwright script を追加し P0 E2E を実行 | 実装済み |
| Phase 3.5 | CI を check / unit / integration / e2e に分割し、feature branch では変更パス別に実行 | 実装済み |
| Phase 4 | main push 後に deploy job を接続 | テスト成功後のみ公開環境へ反映 |
| Phase 5 | schedule で health check と smoke test を実行 | 無料枠公開環境の休眠・疎通確認 |

### 6.3 推奨 workflow 構成

- `ci.yml`
  - pull request / push で frontend build, backend test, unit test を実行
- `e2e.yml`
  - pull request / main push で P0 E2E を実行
  - 実行時間が伸びたら main push と手動実行に絞る
- `deploy.yml`
  - main push の CI 成功後に Render deploy hook や static hosting deploy を実行
- `healthcheck.yml`
  - schedule で公開 URL の `/actuator/health` または `/api/health` を確認

## 7. 完了条件

公開前の最小完了条件は次の通り。

- P0 UT が CI で実行される: 達成
- P0 IT が CI で実行される: 達成
- P0 E2E が main push 前後で実行できる: 達成
- 認証追加後、ユーザー A がユーザー B の記事を参照・更新・削除できないことを IT / E2E で確認する: 達成
- DB 初期化、テストデータ、stub の運用が文書化されている: 達成。通常起動では自動投入せず、手動サンプル seed とテスト用一意データを分離済み
- CI 失敗時に公開デプロイが実行されない: 現状 deploy job 未接続のため継続

## 8. 未決事項

- フロントエンド UT は Vitest を採用済み。コンポーネントテストを Vitest で増やすか、Playwright component / E2E に寄せるか
- バックエンド IT は H2 PostgreSQL mode で開始済み。本番互換性を上げるため Testcontainers PostgreSQL に移行するか
- E2E は PR ごとに P0 を実行する構成。件数が増えたら P0 のみ PR、全件は main / nightly にするか
- OGP stub は IT では application port の `@MockBean` 差し替え済み。E2E で HTTP stub server を立てるか
