# ReadStack Project Status

最終更新: 2026-05-07

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
- 記事一覧カードからのお気に入り追加 / 解除
- 記事一覧カードからの未読 / 既読切り替え
- 記事一覧カードの既読切り替えアイコンの未読 / 既読状態別表示
- 記事一覧カードでのステータス切り替え後の元に戻す導線
- 記事一覧カードからの削除
- 記事削除
- レスポンシブを意識した一覧 / 詳細の画面切り替え
- 記事一覧エリアの内部スクロール
- 記事一覧エリアの控えめなスクロールバーとカード幅安定化
- OGP 画像本体を IndexedDB に Blob 保存し、記事カードのサムネイル表示に利用
- Vuetify によるボタン、入力、カード、ダイアログ、チップ、日付入力の共通化
- フロントエンドは `features/articles` と `shared` に分割し、画面構成、API adapter、Pinia store、pure domain helper、共通 API client / utility を分離済み
- `features/auth` にユーザー登録 / ログイン画面、認証 API adapter、access token をメモリ保持する Pinia store を追加済み
- API client は `Authorization` header 付与、refresh cookie 送信、期限前 refresh、`401` 時の refresh / retry に対応済み
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
- OGP 取得は application 層の `ArticleMetadataProvider` ポート経由にし、具体的な HTTP / HTML 解析実装は infrastructure 層へ閉じ込めている

### 2.3 開発環境

- Docker Compose による `frontend` / `backend` / `db` の一括起動
- Vite によるフロントエンドのホットリロード
- Spring Boot DevTools と Maven compile 監視によるバックエンドの自動再起動
- `@playwright/test` によるブラウザ挙動の手動検証
- Vitest によるフロントエンド domain / store の UT
- JUnit 5 / Spring Boot Test / MockMvc / H2 によるバックエンド UT / IT
- Playwright による登録、記事追加、ログアウト / ログイン、ユーザー分離の E2E
- `frontend/scripts/capture-design-screenshots.mjs` による `docs/designs/` の現行スクリーンショット再生成
- GitHub Actions による push / pull request ごとのフロントエンド UT / build、バックエンド UT / IT、E2E

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

- `docs/specification.md` は入口文書化し、`docs/specification/` 配下へ機能・データモデル・API・UI・非機能仕様を分割済み
- `docs/requirements.md` は入口文書化し、`docs/requirements/functional/` と `docs/requirements/non-functional/` 配下へ項目別ファイルを分割済み

## 3. 仕様との差分

現状の実装は要件の中核を概ね満たしていますが、ドキュメント上の理想と比べると次の差分があります。

- テスト戦略は `docs/testing.md` に UT / IT / E2E の目的、スコープ、ケース、CI/CD 方針を整理済み。P0 中心の UT / IT / E2E 実装を追加済み
- ユーザー登録・ログイン・JWT認証は実装済み。DB migration は未導入で、現行は `ddl-auto=update` と起動時初期化で開発DBを補助している
- `article_tags.user_id` と複合 FK による DB レベルの user mismatch 防止は未実装。現行は repository 層で article / tag の userId 一致を検証している
- 無料枠公開・CI/CD構成案は `docs/deployment/free-deployment.md` に整理済みだが、本番用 health check、`PORT` 対応、managed DB 接続、deploy workflow は未実装
- スマホ対応デザイン検討は `docs/design/mobile-responsive.md` に整理済みだが、ハンバーガードロワー、ボトムナビ、全画面追加モーダル、スマホ向けカレンダーは未実装
- OGP サムネイルは記事カードで表示でき、画像本体は IndexedDB に保存して再利用しているが、既存記事への再取得やアプリ外ストレージへの永続保存は未対応
- タグ管理 API はあるが、独立したタグ管理画面はまだない
- 検索 / フィルタ / 並び替えはフロントエンドで組み合わせて使える
- 画像添付、スクリーンショット起点の入力、OCR/AI 系機能は未着手

## 4. 未実装・残作業

優先度ごとに整理した残タスク一覧は `docs/task-backlog.md` を参照してください。ここでは現状との差分として特に重要な未実装事項を残しています。

### 4.1 MVP 仕上げで優先度が高いもの

- エラーハンドリング改善
  - API 失敗時の再試行や入力保持
- 入力体験の改善
  - バリデーションメッセージの見える化
  - 保存中状態の表示
- テスト拡張
  - ユーザー分離の更新 / 削除ケースを IT / E2E に追加
  - OGP stub server や Testcontainers PostgreSQL の採用を検討
  - スマホUI実装後に mobile project の E2E を追加

### 4.2 次の機能追加候補

- タグ管理画面
- OGP サムネイルの再取得や IndexedDB 外への画像ファイル保存
- お気に入り導線の強化
- モバイル操作の磨き込み
- メール確認、パスワードリセット、全端末ログアウト
- 無料枠公開に向けた production 設定、health check、CI/CD deploy 整備

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

- バックエンドは `spring.jpa.hibernate.ddl-auto=update` のため、開発中は便利ですが、本番運用前にはマイグレーション方式へ寄せたいです
- バックエンドは会社プロジェクト向けサンプルとして使いやすいよう、まず OGP 取得の port / adapter 分離を進めました。次はユースケース分割、`TagRepository` 分離、Web DTO と application DTO の境界整理が候補です
- フロントエンドは会社プロジェクト向けサンプルとして使いやすいよう、`App.vue` の薄い shell 化、feature-oriented な記事管理構成、pure function 分離、API client / adapter 分離、CSS ファイル分割を進めました
- Playwright E2E script、Vitest UT script、バックエンド UT / IT を追加済みです。CI は `npm run test:unit`、`npm run build`、`docker compose run --rm backend mvn test`、`npm run test:e2e` を実行します
- Docker Compose は backend の `/actuator/health` が healthy になってから frontend を起動し、Playwright E2E の登録フローがバックエンド起動途中に走らないようにしています
- Dependabot 設定を追加し、npm、Maven、GitHub Actions、Docker、Docker Compose の依存関係を週次で確認する運用にしました
- `docs/designs/` は静的モック固定ではなく、現行実装を撮り直して同期する運用へ移行したため、UI 変更時は project skill `readstack-design-capture` の利用を前提とします
- 検索、複数タグ、おすすめ度、日付範囲、並び替えは主にフロントエンド内で適用しており、記事件数が増えたときの性能検証はこれからです
- `rating` のような新規列追加は既存データがある PostgreSQL では `ddl-auto=update` だけだと失敗しうるため、当面は後方互換を意識しつつ、将来的には明示的なマイグレーション導入が必要です
- 認証追加に伴い `articles.user_id` / `tags.user_id` と user scoped unique 制約を JPA 定義へ追加していますが、既存DBの旧 unique 制約削除や `article_tags` の複合 FK 化は Flyway などの明示的 migration で実施する必要があります

## 6. おすすめの次アクション

1. OGP 取得失敗や API 失敗時の画面表示を改善する
2. 保存中状態やバリデーションメッセージを整えて入力体験を仕上げる
3. Playwright で重要導線の E2E テスト script を整備する
4. 一通りの実装が揃った後にテスト基盤を整備する
