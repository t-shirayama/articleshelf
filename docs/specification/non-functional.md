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
- フロントエンドの確認は `npm run build` で型チェックと Vite ビルドを行う
- ブラウザ挙動の確認には `@playwright/test` を利用できるが、正式なE2Eテストスクリプトは未整備
- バックエンド確認は Docker 経由の Maven コマンドで行う
- テスト: MVP実装が一通り揃った後に単体テスト / 結合テスト / E2Eテストを整備する
