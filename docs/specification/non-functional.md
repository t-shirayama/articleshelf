# 非機能仕様

## 基本方針

- フロントエンド: Vue.js 3 + Pinia（状態管理） + Vuetify
- バックエンド: Spring Boot 3 + Spring Data JPA
- DB: PostgreSQL
- バックエンドとDBは Docker / Docker Compose で起動できる構成とする
- バリデーション: フロントエンドとバックエンド両方で実施
- API形式: JSON
- ロギング: バックエンドでリクエスト/エラーを記録
- テスト: 単体テスト / 結合テスト
