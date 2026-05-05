# ReadStack Architecture

## 1. システム構成

- フロントエンド: Vue.js アプリ（Vue 3 + Vue Router + Pinia）
- バックエンド: Spring Boot アプリ（Spring Web, Spring Data JPA）
- データベース: PostgreSQL
- 通信: REST API（JSON）

## 2. 層構造

### 2.1 フロントエンド

- `pages` / `views`: 画面コンポーネント
- `components`: 共有コンポーネント
- `store`: アプリ状態管理
- `services`: API呼び出しラッパー

### 2.2 バックエンド

バックエンドは DDD / クリーンアーキテクチャの考え方で実装します。
依存関係は内側から外側へ流れるように設計し、ドメインモデルが中心になります。

- `domain`: ドメインモデル、集約、値オブジェクト、ドメインサービス、リポジトリインターフェース
- `application`: ユースケース、アプリケーションサービス、DTO、ポート（インターフェース）
- `infrastructure`: 永続化実装、外部APIクライアント、OGP取得、リポジトリ実装
- `adapter`: Web/API レイヤー、コントローラー、HTTP 入力/出力のマッピング
- `config`: Spring の設定、CORS、Bean 定義

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
- `com.readstack.infrastructure.persistence`
  - `ArticleEntity`
  - `JpaArticleRepository`
- `com.readstack.infrastructure.ogp`
  - `OgpClient`
  - `OgpService`
- `com.readstack.adapter.web`
  - `ArticleController`
  - `TagController`

#### ドメイン中心の実装方針

- `Article` と `Tag` をドメインオブジェクトとして扱う
- URL/OGP取得ロジックはインフラ層で実装し、アプリケーション層のユースケースから呼び出す
- 永続化はリポジトリインターフェース経由で行い、Spring Data JPA 実装はインフラ層に閉じる
- API層は DTO を受け取り、アプリケーションサービスに変換して処理する

## 3. DB設計

### テーブル構成

- `articles`
  - id: UUID
  - url: VARCHAR
  - title: VARCHAR
  - summary: TEXT
  - status: VARCHAR
  - read_date: DATE
  - favorite: BOOLEAN
  - notes: TEXT
  - created_at: TIMESTAMP
  - updated_at: TIMESTAMP

- `tags`
  - id: UUID
  - name: VARCHAR
  - created_at: TIMESTAMP
  - updated_at: TIMESTAMP

- `article_tags`
  - article_id: UUID
  - tag_id: UUID

## 4. APIクライアントフロー

- フロントエンドは `GET /api/articles` で一覧を取得
- 検索やフィルター条件はクエリパラメータで送信
- `POST /api/articles` で記事を追加
- `PUT /api/articles/{id}` で記事を更新
- `GET /api/tags` でタグ一覧を取得
- `POST /api/tags` でタグを追加

## 5. 拡張ポイント

- URLからOGP取得をバックエンドで実装
- ユーザー認証を追加してユーザースコープを分離
- 画像アップロードは将来的な拡張として検討
- AI要約やメタ情報抽出をバックエンドで行う

## 6. 開発環境

- ローカル PostgreSQL
- Spring Boot の `application.yml` で DB URL を設定
- Vue 開発サーバーと Spring Boot サーバーを別プロセスで実行
- CORS 設定をバックエンドで許可
