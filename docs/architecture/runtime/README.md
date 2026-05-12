# Runtime Architecture

## 開発環境

- バックエンドは Docker コンテナでビルド・起動する
- PostgreSQL は Docker Compose で起動し、バックエンドコンテナから接続する
- Spring Boot の `application.yml` では環境変数ベースで DB URL を設定できるようにする
- フロントエンドは Docker Compose 上の開発サーバーで起動し、ソース変更時にホットリロードする
- バックエンドは Docker Compose 上の Spring Boot DevTools と Maven compile 監視により、Java / resources 変更時に自動再起動する
- CORS 設定をバックエンドで許可

採用技術とバージョン方針は [技術スタック](../technology/README.md) に集約する。

## Docker 開発方針

- `backend` サービスに Spring Boot アプリを配置する
- `backend` サービスは backend 用 Dockerfile の `dev` ステージを使い、`backend` ディレクトリをコンテナへマウントする
- `frontend` サービスは frontend 用 Dockerfile の開発ステージを使い、`frontend` ディレクトリをコンテナへマウントする
- `frontend` サービスは Node 公式 image の non-root `node` user で dev server を実行する
- `db` サービスに PostgreSQL を配置する
- バックエンドは `db` をホスト名として DB 接続する
- 開発時は `docker compose up --build` でフロントエンド、バックエンド、DB をまとめて起動できる構成とする
- 本番向けにもコンテナイメージを再利用しやすいよう、Dockerfile はアプリ単体で完結する形を採用する

## 公開 runtime の固定値

- Render 公開構成では repository root の `render.yaml` を backend deploy 定義の正本にする
- `render.yaml` では `SPRING_PROFILES_ACTIVE=prod`、`AUTH_CSRF_ENABLED=true`、`AUTH_COOKIE_SECURE=true`、`AUTH_COOKIE_SAME_SITE=None`、`ARTICLESHELF_INITIAL_USER_ENABLED=false` を固定し、公開時の dev default 混入を防ぐ
- `FRONTEND_ORIGIN`、DB 接続情報、secret は Render dashboard の secret env として投入する

## 初期データ方針

- 通常起動では初期ユーザー、記事、タグを自動投入しない
- 初期 ADMIN が必要な検証環境だけ `ARTICLESHELF_INITIAL_USER_ENABLED=true` を指定する
- E2E 用 Compose は管理者リセットシナリオのために初期 ADMIN を明示的に有効化するが、各テストの記事データはテスト内で一意に作成する

## 認証レート制限の運用前提

- 現行 runtime は backend DB の `auth_rate_limit_buckets` テーブルを共有ストアとして使い、register / login の試行回数を backend instance 間で共有する
- login は `IP + username`、register は `IP` を key とし、username は trim / 小文字化後に扱う
- backend instance を再起動しても window が残っている間は rate limit state を引き継ぐ
- DB 障害時は auth API 自体の成功可否も不安定になるため、rate limit は fail-open にせず backend request failure として扱う
- さらに大規模な公開構成では、DB 共有の application rate limit に加えて reverse proxy や WAF の upstream rate limit を前段に置く
- 対象 API、制限 key、既定値、エラー応答は [アカウント API](../../specs/auth/account-api.md) を正本とする
