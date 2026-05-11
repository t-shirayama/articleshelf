# ReadStack Development Roadmap

## プロジェクト概要

- **目標**: MVP (Minimum Viable Product) 完成
- **期間**: 期限なし（段階的開発）
- **チーム**: 1人（フルスタック）
- **優先**: コア機能 + 検索・フィルター + ユーザー認証

## 実装フェーズ

### Phase 1: バックエンド基盤構築（週1-2）

**目標**: Spring Boot API サーバーの基本実装とOGP取得機能

- [ ] Spring Boot プロジェクト初期化
  - Gradle / Maven 構成
  - Spring Web, Spring Data JPA, PostgreSQL ドライバ
- [ ] DDD基本構造の実装
  - `domain/article` パッケージ: Article 集約、ValueObject
  - `application` パッケージ: ユースケース、DTO
  - `infrastructure` パッケージ: JPA リポジトリ実装
- [ ] PostgreSQL ローカル環境セットアップ
- [ ] DB マイグレーション（Flyway / Liquibase）
  - articles テーブル
  - tags テーブル
  - article_tags テーブル
- [ ] REST API エンドポイント実装
  - `POST /api/articles` (記事追加)
  - `GET /api/articles` (一覧取得)
  - `GET /api/articles/{id}` (詳細取得)
  - `PUT /api/articles/{id}` (更新)
  - `DELETE /api/articles/{id}` (削除)
  - `GET /api/tags` (タグ一覧)
  - `POST /api/tags` (タグ追加)
- [ ] OGP 取得サービス実装
  - jsoup / HtmlUnit で URL からメタ情報抽出
  - バックエンドで自動キャッシング
- [ ] ユニットテスト（domain / application layer）

### Phase 2: フロントエンド基本実装（週3-4）

**目標**: Vue.js で基本画面を実装し、バックエンドと統合

- [ ] Vue.js + Vite プロジェクト初期化
- [ ] ルーティング / ページ構成
  - 記事一覧ページ
  - 記事詳細ページ
  - 記事追加モーダル
- [ ] コンポーネント実装
  - ArticleCard / ArticleList
  - ArticleDetail
  - AddArticleModal
  - SearchBar / FilterPanel
- [ ] API クライアント実装
  - Axios / Fetch API ラッパー
  - エラーハンドリング
- [ ] 状態管理 (Pinia)
  - article store
  - tag store
  - UI state
- [ ] スタイリング (Tailwind CSS)
  - レスポンシブ対応
  - モバイル表示
- [ ] 統合テスト（API + UI）

### Phase 3: 認証・検索・テスト完善（週5-6）

**目標**: ユーザー認証、検索・フィルター機能、テスト完全化

- [ ] ユーザー認証実装
  - バックエンド: Spring Security + JWT
  - フロントエンド: ログイン/ログアウト UI
  - 認証トークンの保存と自動更新
- [ ] 記事検索・フィルター機能
  - タイトル・URL・メモの全文検索
  - ステータス（未読/読了）フィルター
  - タグ複数選択フィルター
- [ ] E2E テスト実装
  - Cypress / Playwright で主要フロー検証
- [ ] パフォーマンス最適化
  - ページネーション
  - キャッシング戦略
- [ ] エラーハンドリング・ログ改善

## 環境・ツール

- **バックエンド**: Spring Boot 4, Java 25
- **フロントエンド**: Vue 3, Vite
- **DB**: PostgreSQL 14+
- **コンテナ**: Docker + Docker Compose
- **テスト**: JUnit5, Mockito, Vitest, Cypress
- **VCS**: Git / GitHub

## 注記

- 各フェーズ間で動作確認（統合テスト）を実施
- OGP 取得は外部APIの負荷を考慮し、キャッシング必須
- 認証は後段でOAuth対応に拡張可能な設計に
- 画像ストレージは将来的に S3 対応を想定（現在: メタデータのみ保存）
