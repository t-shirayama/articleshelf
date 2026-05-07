# 非機能仕様

## 基本方針

- フロントエンド: Vue.js 3 + TypeScript + Pinia（状態管理） + Vuetify
- バックエンド: Spring Boot 3 + Spring Data JPA
- DB: PostgreSQL
- フロントエンド、バックエンド、DBは Docker / Docker Compose で起動できる構成とする
- バリデーション: フロントエンドとバックエンド両方で実施
- API形式: JSON
- フロントエンドは Vite のホットリロードに対応する
- バックエンドは Spring Boot DevTools と Maven compile 監視により変更後に再起動する
- OGP画像本体はフロントエンドの IndexedDB に Blob としてキャッシュし、取得失敗URLは一定時間再試行を抑制する
- フロントエンドの確認は `npm run test:unit` と `npm run build` で UT、型チェック、Vite ビルドを行う
- ブラウザ E2E は `@playwright/test` を使い、`npm run test:e2e` で実行する
- バックエンド確認は `docker compose run --rm backend mvn test` で UT / IT を行う
- テスト: P0 中心の単体テスト / 結合テスト / E2Eテストを整備済み。拡張方針は `docs/testing.md` に従う
