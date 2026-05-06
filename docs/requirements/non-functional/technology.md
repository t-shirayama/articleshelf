# 技術スタックの非機能要件

- フロントエンドは Vue.js と TypeScript を使用する
- フロントエンドは Pinia で状態管理し、Vuetify とカスタムCSSでUIを構成する
- バックエンドは Java / Spring Boot を使用する
- データベースは PostgreSQL を使用する
- フロントエンドとバックエンドは REST API で分離する
- 開発環境は Docker Compose で frontend / backend / db をまとめて起動できるようにする
- Maven を使ったバックエンド確認は Docker 上の backend コンテナ経由で行う
- フロントエンドの型・ビルド確認は `npm run build` で行う
- ブラウザ挙動確認には `@playwright/test` を利用できる
