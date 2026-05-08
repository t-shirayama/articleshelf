# Runtime Architecture

## 開発環境

- バックエンドは Docker コンテナでビルド・起動する
- PostgreSQL は Docker Compose で起動し、バックエンドコンテナから接続する
- Spring Boot の `application.yml` では環境変数ベースで DB URL を設定できるようにする
- フロントエンドは Docker Compose 上の Node.js 22 LTS ベース Vite 開発サーバーで起動し、ソース変更時にホットリロードする
- バックエンドは Docker Compose 上の Spring Boot DevTools と Maven compile 監視により、Java / resources 変更時に自動再起動する
- CORS 設定をバックエンドで許可

## Docker 開発方針

- `backend` サービスに Spring Boot アプリを配置する
- `backend` サービスは Java 21 LTS ベースの Maven `dev` ステージを使い、`backend` ディレクトリをコンテナへマウントする
- `frontend` サービスは Node.js 22 LTS ベースの Vite 開発サーバーを使い、`frontend` ディレクトリをコンテナへマウントする
- `db` サービスに PostgreSQL を配置する
- バックエンドは `db` をホスト名として DB 接続する
- 開発時は `docker compose up --build` でフロントエンド、バックエンド、DB をまとめて起動できる構成とする
- 本番向けにもコンテナイメージを再利用しやすいよう、Dockerfile はアプリ単体で完結する形を採用する

## 初期データ方針

- 通常起動では初期ユーザー、記事、タグを自動投入しない
- 初期 ADMIN が必要な検証環境だけ `READSTACK_INITIAL_USER_ENABLED=true` を指定する
- サンプル記事は `frontend` の `npm run seed:sample` を明示実行したときだけ投入する
- E2E 用 Compose は管理者リセットシナリオのために初期 ADMIN を明示的に有効化するが、各テストの記事データはテスト内で一意に作成する
