# ArticleShelf Test Strategy

最終更新: 2026-05-10

ArticleShelf のテストは、記事を「読んだ知識資産」として安全に登録・検索・更新できることを継続的に保証するために整備する。
この README はテスト文書の入口とし、Unit / Integration / E2E の詳細は配下ディレクトリに分割する。

## 1. 目的

経営・プロダクト観点では、リリース前後に次の品質リスクを下げることを目的にする。

- ユーザーが保存した記事、メモ、タグ、既読履歴を失わない
- 重要な導線である記事追加、一覧検索、詳細編集、削除、カレンダー確認が壊れていない
- 認証追加後に、他ユーザーのデータが見えない
- UI 改修やレスポンシブ対応後も、主要な操作が継続して使える
- CI により、公開ブランチへ壊れた変更が入る可能性を下げる

## 2. テスト方針

ArticleShelf では、実行速度と保守性を重視し、次の比率を目安に整備する。

| 種別 | 主目的 | 件数の目安 | 実行タイミング |
| --- | --- | --- | --- |
| UT | 関数、ドメイン、ユースケース単位の仕様を高速に検証 | 多め | ローカル、PR、push |
| IT | API、DB、Spring 設定、永続化、認証境界を検証 | 中程度 | PR、main push |
| E2E | ブラウザ上の主要ユーザーシナリオを検証 | 少数精鋭 | PR、main push、リリース前 |

E2E は便利だが壊れやすく遅くなりやすい。細かい分岐は UT / IT へ寄せ、E2E は役員説明やリリース判定で「主要導線が動く」と説明できる代表シナリオに絞る。

## 3. 優先度

| 優先度 | 意味 | 対象例 |
| --- | --- | --- |
| P0 | リリース判定に必須。壊れるとデータ消失、情報漏えい、主要導線停止につながる | 記事追加、保存、編集、削除、認証、ユーザースコープ、CI |
| P1 | MVP の信頼性に必要。壊れると体験品質が大きく落ちる | 検索、フィルタ、タグ、カレンダー、Markdown 表示 |
| P2 | 使い勝手や追加機能の品質を上げる | 細かな表示状態、境界値、アクセシビリティ補助 |

## 4. 現在の前提

- 採用技術と推奨バージョンは [技術スタック](../architecture/technology/README.md) に従う
- フロントエンド確認: `npm run build`
- フロントエンド lint: `npm run lint`
- フロントエンド UT: `npm run test:unit`
- フロントエンド UT coverage: `npm run test:unit:coverage`
- フロントエンド bundle size check: `npm run check:bundle`。事前に `npm run build` で `dist/assets` を生成する
- ブラウザ E2E: `npm run test:e2e`
- バックエンド確認: ローカル `mvn` ではなく Docker 経由で `docker compose run --rm backend mvn test` を実行する
- バックエンド PR gate: `docker compose run --rm backend mvn test spotbugs:check` を CI で必須化し、JUnit / PostgreSQL IT / Clean Architecture dependency test / SpotBugs をまとめて確認する
- deployment config gate: `bash scripts/check-render-backend-config.sh` を CI の `Deployment config check` で実行し、`render.yaml` の `SPRING_PROFILES_ACTIVE=prod`、CSRF / secure cookie 固定値、health check path を確認する
- バックエンド UT coverage: `docker compose run --rm backend mvn -Pcoverage test -Dtest='ArticleTest,PasswordPolicyTest,UsernamePolicyTest,ArticleServiceTest,AuthRateLimiterTest,ApiExceptionHandlerTest,JwtTokenServiceTest,OgpRequestGuardTest,ProductionEnvironmentValidatorTest,AuthAndArticleIntegrationTest'`
- Flyway migration、JPA Entity、DB 制約、Repository 検索条件を変更した場合は、JPA validate に加えて PostgreSQL 実体で persistence IT を実行する
- refresh token rotation、pessimistic lock、条件付き update を変更した場合は、auth unit / integration に加えて PostgreSQL 実体での persistence / auth 確認を優先する
- Java、Spring Boot、Mockito、JaCoCo、Surefire、SpotBugs の version を更新した場合は、backend PR gate を再実行し、Mockito javaagent / JaCoCo `argLine` / JVM warning が再発していないか確認する
- CI / CD の段階構成と品質ゲートは [CI / CD Architecture](../architecture/ci-cd/README.md) に従う
- 依存関係とコンテナの脆弱性確認は `codeql.yml` と `supply-chain.yml` で実行し、通常の `ci.yml` とは別 workflow として管理する
- backend request ID filter と metrics instrumentation は unit test と build で確認し、metrics tag に token、username、URL、メモ本文などを含めないことをレビュー観点にする
- 認証インフラ境界では、client IP 解決、rate limit 呼び出し、invalid access token metrics が Controller / token 値から分離されていることを確認する

Backend の品質ゲートは、SpotBugs と Clean Architecture dependency test を早期チェック、domain / application coverage threshold を unit test、PostgreSQL 実体確認を integration test、主要導線確認を E2E に分担する。
Frontend の品質ゲートは、ESLint、型チェック、Vite build、bundle size check、API client / Markdown / domain helper の unit test、主要導線の Playwright E2E、必要に応じた design screenshot capture に分担する。
Frontend unit coverage は lines 43%、statements 42%、functions 35%、branches 29% を現行下限にし、`src/**/*.{ts,vue}` の実装を対象にする。
API client unit test では refresh retry、CSRF header、Accept-Language、error mapping、AbortSignal forwarding、production API base URL validation を確認する。
UI / E2E 確認では、主要 dialog の focus 復帰、記事カードの詳細 open button と右上 action button の独立操作、カレンダー日付セルの keyboard open / close、`prefers-reduced-motion` 時の不要な transition 抑制、認証後記事一覧の axe accessibility scan をアクセシビリティ観点として見る。
ArticleWorkspace の検索 debounce は `useArticleSearchDebounce` の unit test で、最後の入力値だけが反映されることと unmount / logout 相当の cancel で遅延反映されないことを確認する。
Router 導入後の E2E / 手動確認では、未認証 protected route が `/login` へ補正されること、認証後に `/articles` / `/calendar` / `/tags` / `/settings` へ直接入れること、記事カード選択で `/articles/:id` になることを見る。
記事一覧 query model は `ArticleListQueryTest` で、pagination 未指定時に既存の全件レスポンスを維持すること、`page` / `size` の正規化と slice を確認する。
記事追加 preview は backend integration で OGP 成功、OGP 取得不可の partial success、重複 URL、URL validation を確認し、frontend unit で URL 変更、modal close、race condition、duplicate reset、空の title / summary を payload に含めないことを確認する。
Markdown security unit test では、危険タグ、危険属性、危険 scheme、`data:` image、SVG / iframe、malformed HTML、外部リンクの `target` / `rel` を検証する。
Supply chain security は Dependabot の更新 PR、Dependency Review の PR 差分検知、CodeQL の code scanning、Trivy の filesystem / backend image scan に分担する。
Dependency Review は GitHub repository の Dependency graph が有効で、repository variable `DEPENDENCY_REVIEW_ENABLED=true` が設定されている場合に moderate 以上の既知脆弱性を PR gate として扱う。Dependency graph が使えない repository では job 内で skip notice を出し、Trivy / CodeQL / Dependabot を継続する。
PDF インポートを実装する場合は、URL あり / 複数 URL / URL なし / 段組や改行の多い PDF / スキャン PDF / OGP 取得失敗 URL を代表ケースとして確認する。

## 5. 完了条件

リリース前の最小完了条件は達成済み。現在の基準は次の通り。

- P0 UT が CI で実行される: 達成
- P0 IT が CI で実行される: 達成
- P0 E2E が main push 前後で実行できる: 達成
- 認証追加後、ユーザー A がユーザー B の記事を参照・更新・削除できないことを IT / E2E で確認する: 達成
- DB 初期化、テストデータ、stub の運用が文書化されている: 達成。各テスト種別の README で、固定値、test double、一意データの扱いを確認する

## 6. 詳細文書

- [Unit Test](unit/README.md): UT の目的、スコープ、実装方針、実行方法、ルール
- [Unit Test ケース](unit/cases.md): UT ケース一覧と実装済み UT
- [Integration Test](integration/README.md): IT の目的、スコープ、実装方針、実行方法、DB / API / 認証境界の確認ルール
- [Integration Test ケース](integration/cases.md): IT ケース一覧と実装済み IT
- [E2E Test](e2e/README.md): E2E の目的、スコープ、Playwright 実行方針、操作フローのルール
- [E2E Test ケース](e2e/cases.md): E2E シナリオ一覧と実装済み E2E
