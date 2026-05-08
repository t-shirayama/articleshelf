# 無料枠を中心にした公開構成と CI

最終更新: 2026-05-09

## 1. 目的

ArticleShelf の現在の公開構成、環境変数、公開後の運用メモ、CI 方針を整理する。
対象は個人利用、社内説明、ポートフォリオ、小規模検証であり、商用 SLA や高可用性を前提にしない。

無料枠は、容量制限、休眠、起動遅延、バックアップ制限、利用条件変更がある。
構成を見直す際は各サービスの pricing / docs を再確認する。

## 2. 最終構成

![ArticleShelf デプロイ構成図](images/articleshelf-deployment-overview.png)

上の図は、現在の公開構成の全体像を示す。
現時点の運用では Cloudflare Pages / Render の Git 連携による自動デプロイを使う。
GitHub Actions は CI に加えて、Render Free Web Service の休眠を抑える定期 health check を実行する。

| レイヤー         | 採用サービス                          | 役割                                                             |
| ---------------- | ------------------------------------- | ---------------------------------------------------------------- |
| フロントエンド   | Cloudflare Pages                      | Vue / Vite の `dist` を静的配信する                              |
| バックエンド API | Render Free Web Service               | Spring Boot API を Docker / Java アプリとして公開する            |
| DB               | Supabase Free PostgreSQL              | users、articles、tags、article_tags、refresh_tokens を永続化する |
| CI / 運用ジョブ  | GitHub Actions                        | build / test / coverage / E2E と Render health check を実行する   |
| 公開反映         | Cloudflare Pages / Render の Git 連携 | `main` 更新で各 PaaS の auto deploy が走る                       |

```text
User Browser
  |
  | HTTPS
  v
Cloudflare Pages
  |
  | Vue.js SPA
  | HTTPS REST API
  v
Render Free Web Service
  |
  | JDBC over TLS
  v
Supabase Free PostgreSQL
```

## 3. 実装との相性

この構成は現行実装と相性がよい。

- frontend は `VITE_API_BASE_URL` で backend API URL を build 時に注入できる
- frontend API client は `credentials: 'include'` で refresh token cookie を送信できる
- backend は `server.port: ${PORT:8080}` に対応済みで、Render が指定する `PORT` で待ち受けられる
- backend は `server.forward-headers-strategy: framework` を有効化済みで、Render の HTTPS proxy 後段で secure request / forwarded header を扱える
- production profile は datasource、frontend origin、JWT secret、refresh token hash secret を環境変数必須にしている
- production profile は `AUTH_CSRF_ENABLED=true`、`AUTH_COOKIE_SECURE=true`、`AUTH_COOKIE_SAME_SITE=None` を既定にし、frontend / API が別 site になる構成に合っている
- OGP 取得は timeout、User-Agent、SSRF 対策、redirect 再検証、body size 制限、`text/html` 制限に対応済み
- DB schema は Flyway migration と JPA `validate` で管理しており、Supabase PostgreSQL へ起動時 migration を適用できる
- 現行 frontend は Vue Router を使っていないため SPA fallback は必須ではない。history mode の routing を導入した場合は `_redirects` を追加する

## 4. フロントエンド: Cloudflare Pages

### 4.1 設定

| 項目                   | 値                  |
| ---------------------- | ------------------- |
| Root directory         | `frontend`          |
| Framework preset       | `Vue` または `Vite` |
| Build command          | `npm run build`     |
| Build output directory | `dist`              |
| Production branch      | `main`              |

Cloudflare Pages は Vite / Vue の build command と output directory として `npm run build` / `dist` を扱える。
依存関係の install は Pages build が行う前提とし、必要な場合だけ install command に `npm ci` を明示する。

### 4.2 環境変数

| 変数                | 例                                      | 説明                        |
| ------------------- | --------------------------------------- | --------------------------- |
| `VITE_API_BASE_URL` | `https://articleshelf-api.onrender.com` | Render 上の backend API URL |

### 4.3 公開 URL

Cloudflare Pages は `https://<project-name>.pages.dev` の無料 URL を提供する。

```text
https://articleshelf-app.pages.dev
```

### 4.4 SPA fallback

現行実装は Vue Router を使っていないため、現時点では `_redirects` は必須ではない。
Vue Router の history mode を導入した場合は、`frontend/public/_redirects` を追加し、build output にコピーされるようにする。

```text
/* /index.html 200
```

## 5. バックエンド API: Render Free Web Service

### 5.1 設定

| 項目                           | 値                                    |
| ------------------------------ | ------------------------------------- |
| Service type                   | Web Service                           |
| Environment                    | Docker                                |
| Root directory / Build context | `backend`                             |
| Dockerfile                     | `backend/Dockerfile`                  |
| Health check path              | `/actuator/health`                    |
| Public URL                     | `https://<service-name>.onrender.com` |

```text
https://articleshelf-api.onrender.com
```

Render Free Web Service は、一定時間 inbound traffic がないと spin down し、次回アクセス時に起動待ちが発生する。
README やアプリ画面では、次のような注意書きを表示候補にする。

```text
無料ホスティングを利用しているため、初回アクセス時に API 起動まで時間がかかる場合があります。
```

### 5.2 必須環境変数

| 変数                                         | 例                                               | 説明                                                     |
| -------------------------------------------- | ------------------------------------------------ | -------------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`                     | `prod`                                           | 本番 profile                                             |
| `SPRING_DATASOURCE_URL`                      | `jdbc:postgresql://.../postgres?sslmode=require` | Supabase PostgreSQL JDBC URL                             |
| `SPRING_DATASOURCE_USERNAME`                 | `postgres.<project-ref>`                         | Supabase DB user。接続方式により形式が異なる             |
| `SPRING_DATASOURCE_PASSWORD`                 | `********`                                       | Supabase DB password                                     |
| `FRONTEND_ORIGIN`                            | `https://articleshelf-app.pages.dev`             | CORS 許可 origin                                         |
| `JWT_ACCESS_SECRET`                          | `********`                                       | 本番必須。32文字以上、dev値不可                          |
| `AUTH_REFRESH_TOKEN_HASH_SECRET`             | `********`                                       | 本番必須。32文字以上、dev値不可                          |
| `AUTH_CSRF_ENABLED`                          | `true`                                           | 本番必須                                                 |
| `AUTH_COOKIE_SECURE`                         | `true`                                           | HTTPS cookie 必須                                        |
| `AUTH_COOKIE_SAME_SITE`                      | `None`                                           | Cloudflare Pages と Render が別 site のため既定は `None` |
| `ARTICLESHELF_INITIAL_USER_ENABLED`          | `false`                                          | 通常は初期 ADMIN 自動作成を無効化                        |
| `ARTICLESHELF_AUTH_RATE_LIMIT_ENABLED`       | `true`                                           | 登録 / ログイン API の in-memory rate limit              |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `3`                                              | Supabase Free の接続数を圧迫しないため小さめにする       |

本番必須の認証設定:

```text
AUTH_CSRF_ENABLED=true
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=None
FRONTEND_ORIGIN=https://articleshelf-app.pages.dev
JWT_ACCESS_SECRET=<long-random-secret>
AUTH_REFRESH_TOKEN_HASH_SECRET=<long-random-secret>
```

### 5.3 セキュリティ前提

- backend へ外部から直接到達させず、Render の公開 HTTPS 経路を通す
- 認証 rate limit の client IP は Spring / servlet container が確定した remote address を使う
- 現行 rate limit は Render 無料枠の単一 backend インスタンス向け。複数インスタンスへ拡張する場合は Redis、proxy、WAF 側へ移す
- `FRONTEND_ORIGIN` は Cloudflare Pages の production URL を明示し、CORS で `*` は使わない
- secret と DB password は GitHub / Render / Cloudflare の secret 管理に置き、Git へコミットしない

## 6. Database: Supabase Free PostgreSQL

### 6.1 接続方式

Spring Boot は Supabase を DB としてのみ使う。
Supabase Auth、Storage、Edge Functions には依存しない。

推奨接続方式:

- Render から Supabase direct connection の IPv6 が使える場合: direct connection を使う
- IPv6 が使えない、または接続が不安定な場合: Supavisor の Session pooler を使う
- Hibernate / Flyway を使う永続 backend では、prepared statement 非対応の制約がある Transaction pooler は初期候補にしない

JDBC URL 例:

```text
# direct connection
jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require

# session pooler
jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
```

`verify-full` を使う場合は Supabase の CA certificate を取得し、JDBC / runtime に証明書設定を追加する。
現行運用では最低限 `sslmode=require` を必須にし、証明書検証の強化は今後の改善項目として扱う。

### 6.2 Free plan の注意

- Free plan は DB size、disk、egress、project 数、休眠条件に制限がある
- DB size が Free quota を超えると read-only mode になる可能性がある
- Free project は一定期間 inactive だと pause される可能性がある
- 自動 backup は有料 plan と同等ではないため、運用開始時に manual export / restore 手順を確認する

### 6.3 DB 運用

- Supabase project は本番用と開発用を分ける
- Flyway migration は backend 起動時に適用する
- `spring.jpa.hibernate.ddl-auto=validate` を維持し、本番 DB で `update` は使わない
- リリース前後に `/actuator/health` で DB 接続が healthy になることを確認する
- `pg_dump` または Supabase dashboard から export 手順を確認する

## 7. CI 方針

### 7.1 現在の運用

- PR / push では GitHub Actions の CI を実行する
- Cloudflare Pages は Git 連携で `frontend` を build / deploy する
- Render は Git 連携で `backend` を Docker build / deploy する
- 公開反映は Cloudflare Pages / Render 側の auto deploy に任せ、必要時だけ dashboard で rollback / redeploy する

## 8. 公開後の運用チェックリスト

### 8.1 Frontend

- [ ] Cloudflare Pages project を作成した
- [ ] Root directory が `frontend` になっている
- [ ] Build command が `npm run build` になっている
- [ ] Build output directory が `dist` になっている
- [ ] `VITE_API_BASE_URL` が Render API URL を向いている
- [ ] 公開 URL を `FRONTEND_ORIGIN` に反映した
- [ ] Vue Router history mode を導入した場合は `_redirects` を追加した

### 8.2 Backend

- [ ] Render Web Service を Docker で作成した
- [ ] Root directory / build context が `backend` になっている
- [ ] Health check path が `/actuator/health` になっている
- [ ] `SPRING_PROFILES_ACTIVE=prod` を設定した
- [ ] Supabase 接続情報を設定した
- [ ] `FRONTEND_ORIGIN` に Cloudflare Pages production URL を設定した
- [ ] `AUTH_CSRF_ENABLED=true` を設定した
- [ ] `AUTH_COOKIE_SECURE=true` を設定した
- [ ] `AUTH_COOKIE_SAME_SITE=None` を設定した
- [ ] `JWT_ACCESS_SECRET` と `AUTH_REFRESH_TOKEN_HASH_SECRET` に十分長いランダム値を設定した
- [ ] `ARTICLESHELF_INITIAL_USER_ENABLED=false` を確認した
- [ ] `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` を小さめに設定した

### 8.3 Database

- [ ] Supabase project を本番用として作成した
- [ ] Direct connection または Session pooler の JDBC URL を選んだ
- [ ] JDBC URL に `sslmode=require` 以上を指定した
- [ ] Flyway migration が正常に通ることを確認した
- [ ] 開発用データと本番用データが混ざっていない
- [ ] DB password を GitHub / Render / docs にコミットしていない
- [ ] backup / export / restore 手順を確認した

### 8.4 公開後

- [ ] Cloudflare Pages から API に通信できる
- [ ] 登録、ログイン、refresh、logout が cookie / CSRF 込みで動く
- [ ] 記事追加、一覧、詳細、編集、削除が動く
- [ ] OGP 取得が公開環境から動く
- [ ] Render cold start 時の表示が破綻しない
- [ ] Markdown 安全境界が維持されている
- [ ] Supabase の database size / connection usage を確認した

## 9. 未決事項

- custom domain を初期から設定するか
- `sslmode=verify-full` と Supabase CA certificate の導入をいつ行うか

## 10. 公式参照

- Cloudflare Pages build configuration: https://developers.cloudflare.com/pages/configuration/build-configuration/
- Cloudflare Pages redirects: https://developers.cloudflare.com/pages/configuration/redirects/
- Render Deploy for Free: https://render.com/docs/free
- Render Web Services: https://render.com/docs/web-services/
- Supabase pricing: https://supabase.com/pricing
- Supabase database size: https://supabase.com/docs/guides/platform/database-size
- Supabase connection strings: https://supabase.com/docs/reference/postgres/connection-strings
- Supabase SSL enforcement: https://supabase.com/docs/guides/platform/ssl-enforcement
