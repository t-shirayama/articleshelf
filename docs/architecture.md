# ReadStack Architecture

## 1. システム構成

- フロントエンド: Vue.js アプリ（Vue 3 + TypeScript + Pinia + Vuetify）
- バックエンド: Spring Boot アプリ（Spring Web, Spring Data JPA）
- データベース: PostgreSQL
- 実行基盤: Docker / Docker Compose
- 通信: REST API（JSON）

## 2. 層構造

### 2.1 フロントエンド

- `features/articles`: 記事管理機能の画面、コンポーネント、Pinia store、API adapter、ドメイン helper
- `features/auth`: ユーザー登録 / ログイン画面、認証 API adapter、access token を保持する Pinia store
- `features/articles/views`: 一覧 / カレンダー / 詳細を切り替える feature workspace
- `features/articles/components`: 記事カード、詳細、追加モーダル、フィルタ、サイドバーなど記事機能の UI
- `features/articles/domain`: フィルタ、ソート、フォーム変換、API 入力変換など副作用を持たない関数
- `features/articles/api`: 記事 / タグ API を型付きで呼び出す feature adapter
- `shared`: JWT 付与 / refresh retry 対応の API client、共通 UI、IndexedDB キャッシュ、日付 formatting など機能横断の部品
- `styles`: design token、base、layout、controls、feature styles、responsive を責務単位で分割
- `App.vue`: Vuetify アプリの最上位 shell と feature workspace の接続
- `services/api.ts` / `types.ts`: 既存 import 互換の re-export
- `Vuetify`: ボタン、入力、カード、ダイアログ、チップなどのUIコンポーネント

### 2.2 バックエンド

バックエンドは DDD / クリーンアーキテクチャの考え方で実装します。
依存関係は内側から外側へ流れるように設計し、ドメインモデルが中心になります。

- `domain`: ドメインモデル、集約、値オブジェクト、ドメインサービス、リポジトリインターフェース
- `application`: ユースケース、アプリケーションサービス、DTO、外部機能を抽象化するポート（インターフェース）
- `infrastructure`: 永続化実装、外部APIクライアント、OGP取得、リポジトリ実装
- `adapter`: Web/API レイヤー、コントローラー、HTTP 入力/出力のマッピング
- `config`: Spring の設定、CORS、Bean 定義
- `infrastructure.security`: Spring Security、JWT 発行 / 検証、refresh token hash、password encoder

#### DDDの依存関係ルール

- `domain` はどのレイヤーにも依存しない
- `application` は `domain` に依存する
- `infrastructure` は `application` と `domain` のインターフェースに依存する
- `adapter` は `application` のユースケースに依存し、`domain` を直接扱わない
- `dto` は API 入出力とアプリケーション境界のために使う

#### 例: パッケージ構成

- `com.readstack.domain.article`
  - `Article` (集約ルート)
  - `Tag` (値オブジェクト／エンティティ)
  - `ArticleRepository` (インターフェース)
- `com.readstack.application.article`
  - `ArticleService` / `ArticleUseCase`
  - `AddArticleCommand`
  - `ArticleResponse`
  - `ArticleMetadataProvider` (OGP 取得など外部メタデータ取得のポート)
- `com.readstack.infrastructure.persistence`
  - `ArticleEntity`
  - `JpaArticleRepository`
- `com.readstack.infrastructure.ogp`
  - `OgpClient`
  - `OgpService` (ArticleMetadataProvider の実装)
- `com.readstack.adapter.web`
  - `ArticleController`
  - `TagController`

#### ドメイン中心の実装方針

- `Article` と `Tag` をドメインオブジェクトとして扱う
- URL/OGP取得ロジックはインフラ層で実装し、アプリケーション層は `ArticleMetadataProvider` ポート経由で呼び出す
- 永続化はリポジトリインターフェース経由で行い、Spring Data JPA 実装はインフラ層に閉じる
- API層は DTO を受け取り、アプリケーションサービスに変換して処理する

## 3. DB設計

### テーブル構成

- `articles`
  - id: UUID
  - user_id: UUID
  - url: VARCHAR
  - title: VARCHAR
  - summary: TEXT
  - thumbnail_url: VARCHAR
  - status: VARCHAR
  - read_date: DATE
  - favorite: BOOLEAN
  - rating: INTEGER
  - notes: TEXT
  - created_at: TIMESTAMP
  - updated_at: TIMESTAMP

- `tags`
  - id: UUID
  - user_id: UUID
  - name: VARCHAR
  - created_at: TIMESTAMP
  - updated_at: TIMESTAMP

- `article_tags`
  - article_id: UUID
  - tag_id: UUID

- `users`
  - id: UUID
  - email: VARCHAR
  - password_hash: VARCHAR
  - display_name: VARCHAR
  - role: VARCHAR
  - status: VARCHAR
  - created_at: TIMESTAMP
  - updated_at: TIMESTAMP
  - last_login_at: TIMESTAMP

- `refresh_tokens`
  - id: UUID
  - user_id: UUID
  - token_hash: VARCHAR
  - family_id: UUID
  - expires_at: TIMESTAMP
  - revoked_at: TIMESTAMP
  - created_at: TIMESTAMP
  - replaced_by_token_id: UUID
  - user_agent: VARCHAR
  - ip_address: VARCHAR

## 4. APIクライアントフロー

- 未ログイン時は `AuthScreen` を表示し、`POST /api/auth/login` または `POST /api/auth/register` で access token と refresh cookie を受け取る
- access token は Pinia の `features/auth` store にメモリ保持し、API client が `Authorization: Bearer` を付与する
- ページ再読み込み時は `POST /api/auth/refresh` で session を復元する
- access token の期限前 refresh と、`401` 時の refresh / retry を API client が担当する
- フロントエンドは `GET /api/articles` で一覧を取得
- 初回取得では検索、ステータス、お気に入り条件をクエリパラメータで送信できる
- 複数タグ、おすすめ度、登録日範囲、既読日範囲、並び替えは取得後にフロントエンド側の Pinia store で適用する
- `POST /api/articles` で記事を追加
- `PUT /api/articles/{id}` で記事を更新
- 記事一覧カードの未読 / 既読切り替えとお気に入り切り替えは、フロントエンドで楽観的に反映してから `PUT /api/articles/{id}` で保存する
- `GET /api/tags` でタグ一覧を取得
- `POST /api/tags` でタグを追加
- OGP画像はDB上の `thumbnail_url` を直接表示せず、記事カードのサムネイル領域が表示範囲に近づいた時だけフロントエンドが取得し、IndexedDBに画像Blobとして保存したものを表示する
- 画像取得に失敗したURLは一定時間再試行せず、外部サイトへのアクセス増加を避けてプレースホルダー表示に戻す

## 5. Markdown 表示の安全境界

- 詳細画面のメモ Markdown はフロントエンドだけで HTML に変換し、バックエンドには元のメモ本文を保存する
- Markdown 変換では raw HTML を無効化し、ユーザー入力の `<script>` やイベント属性を Markdown として実行可能な HTML にしない
- `v-html` に渡す HTML は必ず DOMPurify を通し、`script` / `iframe` / `object` / `embed` / `style` / フォーム系タグ / SVG / MathML / media 系タグを禁止する
- リンクは `http` / `https` / `mailto` のみ許可し、外部リンクには `target="_blank"` と `rel="noopener noreferrer nofollow"` を付ける
- 画像は `http` / `https` のみ許可し、`data:` や `javascript:` などのスキームは表示しない
- コードブロックは文字列を highlight.js で静的に装飾するだけで、コード本文を評価・実行しない

## 6. 拡張ポイント

- URLからOGP取得はバックエンドで実装済み。今後は手動再取得や保存済み画像の扱いを拡張できる
- ユーザー認証を追加し、記事 / タグ API は user scoped repository で分離済み。`article_tags.user_id` と複合 FK による DB レベルの不一致防止は本番 migration 導入時のフォローとする
- 画像アップロードは将来的な拡張として検討
- AI要約やメタ情報抽出をバックエンドで行う

## 7. 開発環境

- バックエンドは Docker コンテナでビルド・起動する
- PostgreSQL は Docker Compose で起動し、バックエンドコンテナから接続する
- Spring Boot の `application.yml` では環境変数ベースで DB URL を設定できるようにする
- フロントエンドは Docker Compose 上の Vite 開発サーバーで起動し、ソース変更時にホットリロードする
- バックエンドは Docker Compose 上の Spring Boot DevTools と Maven compile 監視により、Java / resources 変更時に自動再起動する
- CORS 設定をバックエンドで許可

### 7.1 Docker 開発方針

- `backend` サービスに Spring Boot アプリを配置する
- `backend` サービスは開発時に Maven ベースの `dev` ステージを使い、`backend` ディレクトリをコンテナへマウントする
- `frontend` サービスは Vite 開発サーバーを使い、`frontend` ディレクトリをコンテナへマウントする
- `db` サービスに PostgreSQL を配置する
- バックエンドは `db` をホスト名として DB 接続する
- 開発時は `docker compose up --build` でフロントエンド、バックエンド、DB をまとめて起動できる構成とする
- 本番向けにもコンテナイメージを再利用しやすいよう、Dockerfile はアプリ単体で完結する形を採用する
