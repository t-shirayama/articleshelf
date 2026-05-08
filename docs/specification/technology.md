# 技術スタック仕様

ReadStack の実装で採用する主要技術と、開発・CI で揃える実行環境を定義する。
非機能要件そのものは [non-functional.md](non-functional.md) に整理する。

## 1. アプリケーション構成

- フロントエンド: Vue.js 3 + TypeScript
- 状態管理: Pinia
- UI: Vuetify とカスタム CSS
- バックエンド: Java 21 + Spring Boot 4
- 永続化: Spring Data JPA + Flyway
- DB: PostgreSQL
- API 形式: REST API / JSON

## 2. 推奨バージョン

- Node.js: 22 LTS
- Java: 21 LTS
- PostgreSQL: 18 系
- Spring Boot: 4.0.x

ローカルの目安として `.nvmrc` は `22`、`.java-version` は `21` を置く。
Docker、CI、lockfile 前提の `npm ci` も同じ基準に揃える。

## 3. 開発環境

- フロントエンド、バックエンド、DB は Docker / Docker Compose で起動できる構成にする
- フロントエンドは Vite のホットリロードに対応する
- バックエンドは Spring Boot DevTools と Maven compile 監視により変更後に再起動する
- Maven を使ったバックエンド確認はローカル Maven ではなく Docker 上の backend コンテナ経由で行う

## 4. テストツール

- フロントエンド UT は Vitest を使う
- ブラウザ E2E は `@playwright/test` を使う
- バックエンド UT / IT は JUnit と Spring Boot Test を使う
- バックエンド unit coverage は JaCoCo を使う
- バックエンド静的解析は SpotBugs を使う

具体的なテスト範囲、コマンド、CI の段階実行は [docs/testing/README.md](../testing/README.md) に従う。
