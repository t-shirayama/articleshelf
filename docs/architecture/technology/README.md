# Technology Stack

ArticleShelf の実装、開発環境、テストで採用する主要技術を定義する。
公開インフラ、環境変数、デプロイ運用の詳細は [デプロイ構成](../../deployment/README.md) に従う。

## 1. アプリケーション構成

| 領域 | 採用技術 |
| --- | --- |
| フロントエンド | Vue.js 3 + TypeScript |
| 状態管理 | Pinia |
| UI | Vuetify + カスタム CSS |
| バックエンド | Java 21 + Spring Boot 4 |
| 永続化 | Spring Data JPA + Flyway |
| DB | PostgreSQL |
| API | REST API / JSON |

## 2. 推奨バージョン

| 対象 | バージョン |
| --- | --- |
| Node.js | 22 LTS |
| Java | 21 LTS |
| PostgreSQL | 18 系 |
| Spring Boot | 4.0.x |

ローカルの目安として `.nvmrc` は `22`、`.java-version` は `21` を置く。
Docker、CI、lockfile 前提の `npm ci` も同じ基準に揃える。

## 3. 開発環境

- フロントエンド、バックエンド、DB は Docker / Docker Compose で起動できる構成にする
- フロントエンドは Vite のホットリロードに対応する
- バックエンドは Spring Boot DevTools と Maven compile 監視により変更後に再起動する
- Maven を使ったバックエンド確認はローカル Maven ではなく Docker 上の backend コンテナ経由で行う

## 4. テストツール

| 領域 | 採用技術 |
| --- | --- |
| フロントエンド UT | Vitest |
| フロントエンド component test helper | Vue Test Utils / Pinia testing helper |
| ブラウザ E2E | Playwright |
| バックエンド UT / IT | JUnit / Spring Boot Test |
| バックエンド coverage | JaCoCo |
| バックエンド静的解析 | SpotBugs |

具体的なテスト範囲、コマンド、CI の段階実行は [テスト戦略](../../testing/README.md) に従う。
