# ArticleShelf Architecture

ArticleShelf のアーキテクチャ全体像と、詳細文書への入口です。

## システム構成

- フロントエンド: ブラウザで動作する SPA
- バックエンド: REST API を提供するアプリケーション
- データベース: アプリケーションデータの永続化基盤
- 実行基盤: ローカル開発と検証用のコンテナ構成
- 通信: REST API（JSON）

採用技術、推奨バージョン、開発環境、テストツールの詳細は [技術スタック](technology/README.md) に集約する。

## 詳細文書

- [技術スタック](technology/README.md): 採用技術、推奨バージョン、開発環境、テストツール
- [フロントエンド詳細](frontend/README.md): feature 構成、shared 層、UI / API adapter の責務
- [バックエンド詳細](backend/README.md): DDD / クリーンアーキテクチャ、依存関係ルール、パッケージ構成
- [データモデル](data/README.md): PostgreSQL の主要テーブルと関連
- [API / クライアントフロー](api-flow/README.md): 認証、記事 / タグ API、OGP 画像取得の流れ
- [実行環境](runtime/README.md): Docker Compose、開発環境、コンテナ方針
- [CI / CD](ci-cd/README.md): GitHub Actions の段階構成、品質ゲート、デプロイとの関係
- [Architecture Decision Records](adrs/README.md): 主要な設計判断、代替案、トレードオフ

## 構成図

- [フロントエンド構成図](images/articleshelf-frontend-architecture.svg): `app`、`features`、`shared`、`styles` の責務と API / design docs への接続
- [バックエンド依存関係図](images/articleshelf-backend-architecture.svg): DDD / クリーンアーキテクチャのレイヤー、Port、Infrastructure、Config の依存方向

## 層構造の基本方針

- フロントエンドは `features/*` を機能単位の中心にし、横断部品は `shared` と `styles` に分ける
- バックエンドは `domain` を中心に置き、`application`、`infrastructure`、`adapter`、`config` を外側の層として扱う
- API 入出力は DTO / adapter で受け止め、ドメインモデルや永続化実装の詳細を直接露出しない
- 永続化は PostgreSQL を正とし、記事 / タグ / ユーザー / refresh token を user scoped に扱う
- Markdown はフロントエンドだけで HTML 化し、保存値は元のメモ本文を維持する

## 依存関係チェック

- `backend/src/test/java/com/articleshelf/architecture/CleanArchitectureDependencyTest.java` で、バックエンドのクリーンアーキテクチャ依存関係を検査する
- GitHub Actions の `backend-check` job は `docker compose run --rm backend mvn test -Dtest=CleanArchitectureDependencyTest` を実行する
- Maven を使う確認はローカル `mvn` ではなく Docker 経由で実行する
