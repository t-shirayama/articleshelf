# ReadStack Project Status

最終更新: 2026-05-08

## 1. 現在の状況

ReadStack は、記事の登録・一覧表示・カレンダー表示・詳細編集・削除までを一通り触れる MVP の土台ができています。  
フロントエンドは Vue 3 + TypeScript + Pinia + Vuetify で一覧画面、カレンダー画面、詳細画面を切り替える操作フローが実装されており、バックエンドは Spring Boot + PostgreSQL で REST API と永続化が動く構成です。
`docker-compose.yml` には `frontend` / `backend` / `db` が定義されており、ローカルでまとめて起動できます。フロントエンドとバックエンドは開発時のホットリロードにも対応しています。
ユーザー登録・ログイン・JWT認証を実装し、記事 / タグ API はログインユーザーのデータだけを扱う構成になっています。

## 2. 実装済み

### 2.1 フロントエンド

- 記事一覧表示
- タイトル / URL / 概要 or メモ / タグ / 登録日 / 既読日 / ステータスの表示
- 検索キーワードによる絞り込み
- 未読 / 既読 / お気に入りの切り替え
- 登録日 / 更新日 / 既読日 / タイトル / おすすめ度での並び替え
- 上部フィルタモーダルからのタグ複数選択、おすすめ度複数選択、登録日範囲、既読日範囲による絞り込み
- 適用中フィルタ条件の一覧上部表示
- カレンダー画面での登録日 / 既読日モード切り替え
- カレンダー画面での月ごとの追加数 / 既読数 / 積読差分表示
- カレンダー画面での土曜 / 日曜の色分け
- カレンダー上の記事からの記事詳細遷移
- サイドバー下部の画像付き学習継続メッセージ
- 学習継続メッセージの約100件コメント / 30種類イラストモチーフのローテーション表示
- 記事追加モーダル
- 記事追加モーダルでの選択済みタグ削除 / 既存タグ追加 / 新規タグ入力
- 記事追加モーダルでのおすすめ度登録
- 記事追加モーダルでの「あとで読む」チェックボックスによる未読保存
- 記事追加モーダルでの重複 URL エラー時の登録済み記事詳細への導線
- 記事追加モーダルでの保存中表示、既読日を含むバリデーションメッセージ、失敗時の入力保持
- 記事選択後の詳細画面での編集
- 記事詳細画面での閲覧 / 編集モード切り替え
- 記事詳細画面での閲覧 / 編集切り替え時の本文幅安定化
- 記事詳細画面の編集モードで、メモ入力を先頭に置き、「記事の詳細」をデフォルト閉じのアコーディオンにした追記しやすいフォーム配置
- 記事詳細画面でメモ未入力時の空欄表示
- 記事詳細画面の閲覧モードでのメモ Markdown 表示、コードハイライト、ファイル名 / 行番号表示
- Markdown 表示の raw HTML 無効化、DOMPurify サニタイズ、リンク/画像スキーム制限、コード非実行の安全対策
- 記事詳細画面右上の保存 / 削除アイコンボタン
- 記事詳細画面での選択済みタグ削除 / 既存タグ追加 / 新規タグ入力
- 記事詳細画面でのおすすめ度編集
- 記事詳細画面で未保存差分がある状態の画面遷移警告
- 記事詳細画面での保存中 / 削除中表示、保存失敗時の編集継続と再試行
- 記事一覧カードからのお気に入り追加 / 解除
- 記事一覧カードからの未読 / 既読切り替え
- 記事一覧カードの既読切り替えアイコンの未読 / 既読状態別表示
- 記事一覧カードでのステータス切り替え後の元に戻す導線
- 記事一覧カードからの削除
- 記事削除
- 独立したタグ管理画面でのタグ検索、タグ単独作成、リネーム、統合、未使用タグ削除、タグ記事一覧への遷移
- 記事一覧データ取得失敗時の再試行ボタン
- レスポンシブを意識した一覧 / 詳細の画面切り替え
- 記事一覧エリアの内部スクロール
- 記事一覧エリアの控えめなスクロールバーとカード幅安定化
- OGP 画像本体を IndexedDB に Blob 保存し、記事カードのサムネイル表示に利用
- Vuetify によるボタン、入力、カード、ダイアログ、チップ、日付入力の共通化
- フロントエンドは `features/articles` と `shared` に分割し、画面構成、API adapter、Pinia store、pure domain helper、共通 API client / utility を分離済み
- `features/auth` にユーザー登録 / ログイン画面、認証 API adapter、access token をメモリ保持する Pinia store を追加済み
- ログイン / ユーザー登録画面での入力バリデーションメッセージと送信中表示を追加済み
- API client は `Authorization` header 付与、refresh cookie 送信、期限前 refresh、`401` 時の refresh / retry に対応済み
- フロントエンドの API client は 4xx の API `messages`、重複記事 ID、通信失敗、401 fallback、5xx、malformed response を構造化エラーとして扱い、store / composable が画面ごとの error state へ変換する
- ログアウト導線をサイドバーに追加済み
- `styles.css` は import hub 化し、design token、base、layout、controls、記事カード、詳細、フォーム / ダイアログ、レスポンシブ CSS をファイル分割済み

### 2.2 バックエンド

- `GET /api/articles`
- `GET /api/articles/{id}`
- `POST /api/articles`
- `PUT /api/articles/{id}`
- `DELETE /api/articles/{id}`
- `GET /api/tags`
- `POST /api/tags`
- URL 重複チェック
- OGP 取得によるタイトル / 概要 / 画像 URL の補完
- 記事追加時のアクセス不可 URL / エラー応答 URL の登録ブロック
- PostgreSQL への永続化
- CORS 設定
- Spring Security による JWT 認証
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/users/me`
- refresh token rotation と HMAC-SHA256 hash 保存
- 起動時に初期 owner ユーザーを作成し、既存記事 / タグの `user_id` 未設定データへ owner を付与
- 記事 / タグ API の user scoped query
- バリデーションエラー / ドメイン例外の API 応答
- `ApiExceptionHandler` による認証、入力不正、ドメイン例外、想定外例外の統一 JSON エラー応答
- OGP 取得は application 層の `ArticleMetadataProvider` ポート経由にし、具体的な HTTP / HTML 解析実装は infrastructure 層へ閉じ込めている

### 2.3 開発環境

- Docker Compose による `frontend` / `backend` / `db` の一括起動
- Vite によるフロントエンドのホットリロード
- Spring Boot DevTools と Maven compile 監視によるバックエンドの自動再起動
- `@playwright/test` によるブラウザ挙動の手動検証
- Vitest によるフロントエンド domain / store の UT
- Vitest + coverage-v8 によるフロントエンド UT coverage report
- Markdown 表示の安全化ロジックに対する Vitest UT
- JUnit 5 / Spring Boot Test / MockMvc / H2 によるバックエンド UT / IT
- JaCoCo によるバックエンド UT coverage report
- PostgreSQL 実体 + Flyway migration を使う persistence IT
- SpotBugs によるバックエンド静的解析
- Playwright による登録、重複 URL、詳細編集、削除、既読 / 未読切り替え、お気に入り、複合フィルタ、ソート、カレンダー、Markdown 表示、未保存警告、タグ管理操作、ログアウト / ログイン、ユーザー分離の E2E
- `frontend/scripts/capture-design-screenshots.mjs` による `docs/designs/screenshots/` の現行スクリーンショット再生成
- GitHub Actions による push / pull request ごとのフロントエンド UT / build、バックエンド UT / IT、E2E
- GitHub Actions でフロントエンドの `typecheck` とバックエンドの `spotbugs:check` を独立 step として実行

### 2.4 データモデル

- `Article`
  - `userId`
  - `url`
  - `title`
  - `summary`
  - `status`
  - `readDate`
  - `favorite`
  - `rating`
  - `notes`
  - `createdAt`
  - `updatedAt`
- `Tag`
- `User`
- `RefreshToken`
- 記事とタグの関連付け

### 2.5 ドキュメント整理

- `docs/specification/README.md` は入口文書化し、`docs/specification/` 配下へ機能・データモデル・API・UI・非機能仕様を分割済み
- `docs/requirements/README.md` は入口文書化し、`docs/requirements/functional/` と `docs/requirements/non-functional/` 配下へ項目別ファイルを分割済み
- `docs/architecture/README.md` は入口文書化し、`docs/architecture/` 配下へ frontend / backend / data-model / API flow / security / runtime を分割済み
- `docs/designs/README.md` は UI 方針と現行画面キャプチャ説明を統合し、スクリーンショットは `docs/designs/screenshots/` 配下へ集約済み
- `docs/README.md` をドキュメント全体の入口にし、`docs/` 直下の文書はカテゴリ別ディレクトリへ移動済み

## 3. 仕様との差分

現状の実装は要件の中核を概ね満たしていますが、ドキュメント上の理想と比べると次の差分があります。

- テスト戦略は `docs/testing/README.md` に UT / IT / E2E の目的、スコープ、ケース、CI/CD 方針を整理済み。P0 中心の UT / IT / E2E 実装を追加済み
- Markdown の raw HTML / `javascript:` / 危険属性を落とす UT、PostgreSQL 実体の migration / 制約 IT、主要 UI 導線の E2E を追加し、P0 導線の抜けをかなり埋めた
- ユーザー登録・ログイン・JWT認証は実装済み。DB は Flyway migration と JPA `validate` に切り替え、起動時初期化は legacy 開発DB 補助に限定した
- `article_tags.user_id` と複合 FK による DB レベルの user mismatch 防止を実装し、repository 層の userId 一致検証と二重化した
- 無料枠公開・CI/CD構成案は `docs/deployment/free-deployment.md` に整理済み。`PORT` 対応、Actuator health の公開設定、managed DB 向け JDBC URL 前提、production profile の起動検証は実装済みで、deploy workflow だけ未着手
- スマホ対応デザイン検討は `docs/designs/mobile-responsive.md` に整理済みだが、ハンバーガードロワー、ボトムナビ、全画面追加モーダル、スマホ向けカレンダーは未実装
- OGP サムネイルは記事カードで表示でき、画像本体は IndexedDB に保存して再利用しているが、既存記事への再取得やアプリ外ストレージへの永続保存は未対応
- タグ管理画面は実装済みで、タグ検索、単独作成、リネーム、統合、未使用タグ削除、タグ記事一覧遷移を E2E で確認している
- 検索 / フィルタ / 並び替えはフロントエンドで組み合わせて使える
- 画像添付、スクリーンショット起点の入力、OCR/AI 系機能は未着手

## 4. 未実装・残作業

優先度ごとに整理した残タスク一覧は `docs/status/task-backlog.md` を参照してください。ここでは現状との差分として特に重要な未実装事項を残しています。

### 4.1 MVP 仕上げで優先度が高いもの

- テスト拡張
  - OGP stub server や Testcontainers PostgreSQL の採用を検討
  - スマホUI実装後に mobile project の E2E を追加

### 4.2 次の機能追加候補

- OGP サムネイルの再取得や IndexedDB 外への画像ファイル保存
- お気に入り導線の強化
- モバイル操作の磨き込み
- メール確認、パスワードリセット、全端末ログアウト
- 無料枠公開に向けた CI/CD deploy 整備

### 4.3 将来拡張

- Chrome 拡張機能からのクイック登録
  - 閲覧中ページを未読として即登録
  - 閲覧中ページを既読として即登録
- 画像 / スクリーンショット添付
- OCR による記事情報抽出
- AI 要約 / ハイライト
- アカウント管理、退会処理、ユーザーデータ削除ポリシー
- クラウド同期

## 5. 技術的な注意点

- バックエンドは Flyway で schema を管理し、JPA は `validate` のみ使います。`backend/src/main/resources/db/migration/V1__baseline_schema.sql` が初回基準です
- バックエンドは会社プロジェクト向けサンプルとして使いやすいよう、まず OGP 取得の port / adapter 分離を進めました。次はユースケース分割、`TagRepository` 分離、Web DTO と application DTO の境界整理が候補です
- フロントエンドは会社プロジェクト向けサンプルとして使いやすいよう、`App.vue` の薄い shell 化、feature-oriented な記事管理構成、記事操作 / タグ操作 / 詳細フォームの feature composable 分離、pure function 分離、feature 固有 data 分離、API client / adapter 分離、i18n locale 分割、CSS ファイル分割を進めました
- Playwright E2E script、Vitest UT / integration script、バックエンド UT / IT を追加済みです。CI は `check -> unit -> integration -> e2e` の段階実行に分割し、`main` / `develop` では全ジョブ、それ以外のブランチでは変更パスに応じた backend / frontend / E2E 関連ジョブを実行します
- E2E 失敗時は CI で Compose logs と Playwright report / trace artifact を回収するようにしたため、再現調査しやすくなりました
- Node.js は Docker / CI / `.nvmrc` を 22 LTS に揃え、Java は Docker / POM / `.java-version` を 21 LTS に揃えています
- Playwright E2E は `docker-compose.e2e.yml` を使って実行専用コンテナ群を起動し、backend の `/actuator/health` が healthy になってから frontend を起動するようにしています
- production profile では `AUTH_CSRF_ENABLED=true` を必須にし、refresh / logout の cookie 認証を CSRF token で保護する前提を起動時 validation と公開準備 docs に反映しました
- access token の JWT 発行 / 検証は自前の HMAC / JSON / 署名比較実装から `spring-security-oauth2-jose` の encoder / decoder へ移行し、改ざん、期限切れ、想定外 `alg` を UT で確認するようにしました
- PostgreSQL 18 公式イメージはデータ永続化パスの標準が `/var/lib/postgresql/data` から `/var/lib/postgresql` へ変わっているため、Compose の volume mount も新仕様に合わせています。旧 `postgres-data` volume との衝突を避けるため、開発用 DB は `postgres-data-v18` volume を使います
- Dependabot 設定を追加し、npm、Maven、GitHub Actions、Docker、Docker Compose の依存関係を週次で確認する運用にしました
- `docs/designs/screenshots/` は静的モック固定ではなく、現行実装を撮り直して同期する運用へ移行したため、UI 変更時は project skill `readstack-design-capture` の利用を前提とします
- 検索、複数タグ、おすすめ度、日付範囲、並び替えは主にフロントエンド内で適用しており、記事件数が増えたときの性能検証はこれからです
- `rating` のような新規列追加や既存制約整理は Flyway migration へ積む前提になりました。auth 導入前のかなり古いローカルDBは再作成または手動移行が必要な可能性があります
- 認証の回帰防止として、backend IT と Playwright E2E に「他ユーザーの更新 / 削除拒否」ケースを追加しています
- テストは H2 ベースの軽量 IT に加えて、PostgreSQL 実体の persistence IT を少数追加し、Flyway migration・user scoped unique 制約・tag 置換を直接確認しています

## 6. おすすめの次アクション

1. OGP 取得失敗時の補助表示や再試行体験を詰める
2. deploy workflow と公開先 secret 設計を固める
3. Testcontainers PostgreSQL や OGP stub server を導入して migration / 外部取得まわりの検証を厚くする
