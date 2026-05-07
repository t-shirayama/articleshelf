# ReadStack Test Strategy

最終更新: 2026-05-07

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
- ブラウザ確認: `@playwright/test` を利用可能
- バックエンド: Java 21 + Spring Boot + Spring Data JPA
- DB: PostgreSQL
- バックエンド確認: ローカル `mvn` ではなく Docker 経由で実行する
- 既存 CI: `.github/workflows/ci.yml` でフロントエンドビルドとバックエンドパッケージ確認を実行済み

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

現時点の `frontend/package.json` には Vitest がないため、UT 本格整備時は `vitest`, `@vue/test-utils`, `jsdom` を追加する。
バックエンドは `spring-boot-starter-test` が未追加のため、UT / IT 整備時に `backend/pom.xml` へ追加する。

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
| IT-AUTH-001 | P0 | 認証追加後 | 未ログイン | 保護 API は `401` |
| IT-AUTH-002 | P0 | 認証追加後 | ユーザー A の token でユーザー B の記事 ID | `404` または `403` |
| IT-AUTH-003 | P0 | 認証追加後 | refresh token rotation | 旧 refresh token は再利用不可 |
| IT-OPS-001 | P0 | health check | DB 接続正常 | `2xx` |
| IT-OPS-002 | P1 | CORS | 許可 origin | 認証 cookie / header を送れる |

## 5. E2E: End-to-End Test

### 5.1 目的

E2E は、ユーザーがブラウザで使う主要導線を、フロントエンド、バックエンド、DB を結合して検証する。
経営説明では「公開前の代表操作は自動で担保している」と示せるテスト群にする。

### 5.2 スコープ

- Vite frontend
- Spring Boot backend
- PostgreSQL
- Playwright による Chromium 実行
- デスクトップ幅とスマホ幅の代表確認
- 認証追加後の登録、ログイン、ログアウト、ユーザースコープ

### 5.3 対象外

- 外部サイトの OGP 安定性
- メール送信の実配送
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
| E2E-009 | P0 | ユーザー登録 | メール・パスワード登録 | ログイン状態で空の一覧へ遷移 |
| E2E-010 | P0 | ログイン / ログアウト | ログイン、記事追加、ログアウト | ログアウト後は保護画面に入れない |
| E2E-011 | P0 | ユーザー分離 | ユーザー A と B で記事登録 | 互いの記事が一覧に出ない |
| E2E-012 | P1 | スマホ一覧 | 375px 幅で一覧、メニュー、追加 | ハンバーガー、ボトムナビ、全画面モーダルが使える |
| E2E-013 | P1 | スマホ詳細 | 375px 幅で詳細編集 | 右カラム情報が縦積みで操作できる |

### 5.5 テストデータ

- E2E ではテストごとに DB を初期化する
- OGP 取得は実外部サイトに依存させず、バックエンド側で test profile の stub を使う
- URL は `https://example.com/readstack/e2e-001` のようにケースごとに一意にする
- 認証追加後はユーザーもケースごとに一意なメールアドレスを使う

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

推奨構成:

- `frontend/e2e/` に spec を配置
- `playwright.config.ts` で desktop と mobile project を定義
- `webServer` で frontend / backend を起動するか、CI では Docker Compose で事前起動する
- スクリーンショット比較は初期段階では必須にせず、主要表示が非空で操作可能なことを優先する

## 6. CI/CD 連携

### 6.1 現在の CI

現在の `.github/workflows/ci.yml` は次を実行する。

- frontend: `npm ci`, `npm run build`
- backend: GitHub Actions runner 上で backend package job を実行

### 6.2 段階的な拡張

| 段階 | 追加内容 | 目的 |
| --- | --- | --- |
| Phase 1 | backend に `spring-boot-starter-test` を追加し、Docker 経由の backend test を実行 | Java の UT / 軽量 IT を CI に載せる |
| Phase 2 | frontend に Vitest を追加し `npm run test:unit` を実行 | domain / store の回帰検知 |
| Phase 3 | Playwright script を追加し P0 E2E を実行 | 主要導線の自動確認 |
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

- P0 UT が CI で実行される
- P0 IT が CI で実行される
- P0 E2E が main push 前後で実行できる
- 認証追加後、ユーザー A がユーザー B の記事を参照・更新・削除できないことを IT / E2E で確認する
- DB 初期化、テストデータ、stub の運用が文書化されている
- CI 失敗時に公開デプロイが実行されない

## 8. 未決事項

- フロントエンド UT に Vitest を採用するか、コンポーネントテストを Playwright に寄せるか
- バックエンド IT で Testcontainers を使うか、Docker Compose 起動済み DB を使うか
- E2E を PR ごとに全件実行するか、P0 のみ PR、全件は main / nightly にするか
- OGP stub を application port の差し替えで行うか、HTTP stub server で行うか
