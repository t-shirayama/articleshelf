# 無料枠を中心にした公開構成とデプロイ運用

最終更新: 2026-05-12

## 1. 目的

ArticleShelf の現在の公開構成、環境変数、公開後の運用メモ、デプロイ運用を整理する。
CI の段階構成と品質ゲートの詳細は [CI / CD](../architecture/ci-cd/README.md) を正本とする。
対象は個人利用、社内説明、ポートフォリオ、小規模検証であり、商用 SLA や高可用性を前提にしない。

無料枠は、容量制限、休眠、起動遅延、バックアップ制限、利用条件変更がある。
構成を見直す際は各サービスの pricing / docs を再確認する。

## 2. 最終構成

![ArticleShelf デプロイ構成図](images/articleshelf-deployment-overview.png)

上の図は、現在の公開構成の全体像を示す。
現時点の運用では Cloudflare Pages / Render の Git 連携による自動デプロイを使う。
Render Free Web Service の休眠抑制は、Cloudflare Worker から 10 分ごとに health check を送る運用とする。
GitHub Actions は CI を担当し、定期 ping は Cloudflare Worker 側へ分離する。
Render Web Service の固定 env は repository root の `render.yaml` を正本とし、`SPRING_PROFILES_ACTIVE=prod`、CSRF / cookie secure などの公開必須値を Git 管理下で固定する。

| レイヤー         | 採用サービス                          | 役割                                                             |
| ---------------- | ------------------------------------- | ---------------------------------------------------------------- |
| フロントエンド   | Cloudflare Pages                      | Vue / Vite の `dist` を静的配信する                              |
| バックエンド API | Render Free Web Service               | Spring Boot API を Docker / Java アプリとして公開する            |
| DB               | Supabase Free PostgreSQL              | users、articles、tags、article_tags、refresh_tokens を永続化する |
| CI               | GitHub Actions                        | build / test / coverage / E2E を実行する                          |
| 運用ジョブ       | Cloudflare Worker                     | 10 分ごとに Render health check へ ping して休眠を抑制する         |
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
- backend production Docker image は dedicated non-root user で `java -jar` を実行する
- frontend Docker image は開発 / E2E 用でも Node 公式 image の non-root user で dev server を実行する
- backend は `server.forward-headers-strategy: framework` を有効化済みで、Render の HTTPS proxy 後段で secure request / forwarded header を扱える
- production profile は datasource、frontend origin、JWT secret、refresh token hash secret を環境変数必須にしている
- production profile は `AUTH_CSRF_ENABLED=true`、`AUTH_COOKIE_SECURE=true`、`AUTH_COOKIE_SAME_SITE=None` を既定にし、frontend / API が別 site になる構成に合っている
- production profile は `SPRING_DATASOURCE_URL` の `sslmode=require` / `verify-ca` / `verify-full` を起動時に検証し、TLS なしの DB 接続を拒否する
- OGP 取得は timeout、User-Agent、SSRF 対策、redirect 再検証、body size 制限、`text/html` 制限に対応済み
- production profile の OGP 取得は `ARTICLESHELF_OGP_PROXY_URL` で指定した outbound proxy を経由させ、proxy / firewall 側で metadata endpoint と private network 宛て egress を遮断する
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
`frontend/public/_headers` は build output にコピーされ、CSP、nosniff、Referrer-Policy、HSTS、Permissions-Policy を Cloudflare Pages response header として設定する。
local の `vite dev` / `vite preview` は Cloudflare Pages `_headers` をそのまま response header としては返さないため、CSP の差分確認は `frontend/public/_headers` と build 済み `dist/_headers` の一致、および production / preview deploy 上の response header smoke check を正本とする。

### 4.2 環境変数

| 変数                | 例                                      | 説明                        |
| ------------------- | --------------------------------------- | --------------------------- |
| `VITE_API_BASE_URL` | `https://articleshelf-api.onrender.com` | Render 上の backend API URL |

Production frontend runtime では `VITE_API_BASE_URL` を必須とし、未設定の場合は API client の初期化時に失敗させる。
ローカル開発では未設定時だけ `http://localhost:8080` へ fallback する。

### 4.3 公開 URL

Cloudflare Pages は `https://<project-name>.pages.dev` の無料 URL を提供する。

```text
https://articleshelf.pages.dev
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
現行運用では Cloudflare Worker から 10 分ごとに Render の health check URL へ ping し、15 分間アクセスがない場合の停止を抑制している。
Worker 側の cron trigger、送信先 URL、失敗時ログは Cloudflare dashboard で管理し、secret や管理 token は Git へ置かない。
README やアプリ画面で表示する注意書き例:

```text
無料ホスティングを利用しているため、初回アクセス時に API 起動まで時間がかかる場合があります。
```

### 5.2 必須環境変数

| 変数                                         | 例                                               | 説明                                                     |
| -------------------------------------------- | ------------------------------------------------ | -------------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`                     | `prod`                                           | 本番 profile                                             |
| `SPRING_DATASOURCE_URL`                      | `jdbc:postgresql://.../postgres?sslmode=require` | Supabase PostgreSQL JDBC URL。production profile では `sslmode=require` 以上が必須 |
| `SPRING_DATASOURCE_USERNAME`                 | `postgres.<project-ref>`                         | Supabase DB user。接続方式により形式が異なる             |
| `SPRING_DATASOURCE_PASSWORD`                 | `********`                                       | Supabase DB password                                     |
| `FRONTEND_ORIGIN`                            | `https://articleshelf.pages.dev`                 | CORS 許可 origin                                         |
| `JWT_ACCESS_SECRET`                          | `********`                                       | 本番必須。32文字以上、dev値不可                          |
| `AUTH_REFRESH_TOKEN_HASH_SECRET`             | `********`                                       | 本番必須。32文字以上、dev値不可                          |
| `AUTH_CSRF_ENABLED`                          | `true`                                           | 本番必須                                                 |
| `AUTH_COOKIE_SECURE`                         | `true`                                           | HTTPS cookie 必須                                        |
| `AUTH_COOKIE_SAME_SITE`                      | `None`                                           | Cloudflare Pages と Render が別 site のため既定は `None` |
| `ARTICLESHELF_INITIAL_USER_ENABLED`          | `false`                                          | 通常は初期 ADMIN 自動作成を無効化                        |
| `ARTICLESHELF_AUTH_RATE_LIMIT_ENABLED`       | `true`                                           | 登録 / ログイン API の shared DB rate limit              |
| `ARTICLESHELF_OGP_PROXY_URL`                 | `http://ogp-proxy.internal:8080`                 | OGP fetch を通す outbound proxy。production では必須     |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `3`                                              | Supabase Free の接続数を圧迫しないため小さめにする       |

`render.yaml` では `SPRING_PROFILES_ACTIVE=prod`、`AUTH_CSRF_ENABLED=true`、`AUTH_COOKIE_SECURE=true`、`AUTH_COOKIE_SAME_SITE=None`、`ARTICLESHELF_INITIAL_USER_ENABLED=false` を固定し、`FRONTEND_ORIGIN`、DB 接続情報、secret は Render dashboard 側の secret env として入力する。
GitHub Actions の `Deployment config check` は `render.yaml` の固定値を検証し、production profile を外した blueprint が `main` へ入らないようにする。

本番必須の認証設定:

```text
AUTH_CSRF_ENABLED=true
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=None
FRONTEND_ORIGIN=https://articleshelf.pages.dev
JWT_ACCESS_SECRET=<long-random-secret>
AUTH_REFRESH_TOKEN_HASH_SECRET=<long-random-secret>
```

### 5.3 セキュリティ前提

- backend へ外部から直接到達させず、Render の公開 HTTPS 経路を通す
- backend コンテナは final image で root ではなく `articleshelf` user として実行する
- frontend の Docker 開発 / E2E image は root ではなく `node` user として実行する
- 認証 rate limit の client IP は Spring / servlet container が確定した remote address を使う
- OGP fetch は dedicated outbound proxy を経由し、proxy / firewall 側で `169.254.169.254`、`100.100.100.200`、RFC1918 private range、loopback、link-local 宛て egress を遮断する
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
現行運用では最低限 `sslmode=require` を必須にする。production profile の backend は JDBC URL に `sslmode=require`、`verify-ca`、`verify-full` のいずれもない場合は起動を拒否する。

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

## 7. デプロイ連携メモ

### 7.1 現在の運用

- PR / push では GitHub Actions の CI を実行する。CI の詳細は [CI / CD](../architecture/ci-cd/README.md) に従う
- Cloudflare Pages は Git 連携で `frontend` を build / deploy する
- Render は Git 連携で `backend` を Docker build / deploy する
- Render の backend deploy は `render.yaml` の blueprint を起点にし、dashboard 上でも `SPRING_PROFILES_ACTIVE=prod` を外さない
- Cloudflare Worker は 10 分ごとの scheduled event で Render の `/actuator/health` へ ping し、Free Web Service の spin down を抑制する
- 公開反映は Cloudflare Pages / Render 側の auto deploy に任せ、必要時だけ dashboard で rollback / redeploy する

## 8. 公開後の運用チェックリスト

### 8.1 Frontend

- [ ] Cloudflare Pages project を作成した
- [ ] Root directory が `frontend` になっている
- [ ] Build command が `npm run build` になっている
- [ ] Build output directory が `dist` になっている
- [ ] `VITE_API_BASE_URL` が Render API URL を向いている
- [ ] `frontend/public/_headers` の security headers が production deploy に反映されている
- [ ] `style-src 'self'`、`style-src-elem 'self' 'unsafe-inline'`、`style-src-attr 'none'` の CSP 分離が production deploy の response header に反映されている
- [ ] 公開 URL を `FRONTEND_ORIGIN` に反映した
- [ ] Vue Router history mode を導入した場合は `_redirects` を追加した

### 8.2 Backend

- [ ] Render Web Service を Docker で作成した
- [ ] Render 作成時に repository root の `render.yaml` を import した
- [ ] Root directory / build context が `backend` になっている
- [ ] backend final Docker image が non-root user で起動している
- [ ] Health check path が `/actuator/health` になっている
- [ ] `SPRING_PROFILES_ACTIVE=prod` を設定した
- [ ] Supabase 接続情報を設定した
- [ ] `FRONTEND_ORIGIN` に Cloudflare Pages production URL を設定した
- [ ] `AUTH_CSRF_ENABLED=true` を設定した
- [ ] `AUTH_COOKIE_SECURE=true` を設定した
- [ ] `AUTH_COOKIE_SAME_SITE=None` を設定した
- [ ] `JWT_ACCESS_SECRET` と `AUTH_REFRESH_TOKEN_HASH_SECRET` に十分長いランダム値を設定した
- [ ] `ARTICLESHELF_OGP_PROXY_URL` を設定した
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
- [ ] OGP proxy / firewall の deny rule で metadata endpoint と private network 宛て egress が遮断されている
- [ ] Render cold start 時の表示が破綻しない
- [ ] Cloudflare Worker の 10 分ごと ping が Render health check に成功している
- [ ] Markdown 安全境界が維持されている
- [ ] Supabase の database size / connection usage を確認した

## 9. デプロイ運用フロー

現行運用では、手動で artifact をアップロードするのではなく、`main` をデプロイ起点にする。
日常の公開は「作業ブランチで変更をまとめる -> PR で確認する -> `main` へマージする -> Cloudflare Pages / Render の自動デプロイを確認する -> 必要ならタグと GitHub Release を作る」の順で進める。

### 9.1 基本方針

- `main` を本番反映ブランチとして扱う
- Cloudflare Pages と Render はどちらも `main` 更新を検知して自動デプロイする
- 先にタグを切るのではなく、`main` 反映と公開確認を終えてからタグと GitHub Release を作る
- リリースノートは GitHub Release に集約し、タグだけ切って説明が散らばらないようにする

### 9.2 変更を公開するときの標準手順

1. 作業ブランチで実装、関連ドキュメント更新、必要な確認を済ませる
2. PR を作成し、CI の `check -> unit -> integration -> e2e` が通ることを確認する
   deployment 設定を触った場合は `Deployment config check` が `render.yaml` の production 固定値を通していることも確認する
3. PR を `main` にマージする
4. GitHub 連携により Cloudflare Pages と Render の自動デプロイが始まる
5. Cloudflare Pages 側で frontend deploy 成功、Render 側で backend deploy 成功を確認する
6. `https://articleshelf.pages.dev` で主要導線を軽く確認する
7. 問題がなければ、その `main` の commit に対してタグを作成する
8. GitHub Release を作成し、変更点、注意点、既知事項をリリースノートにまとめる

### 9.3 マージ後に確認すること

- Cloudflare Pages の production deploy が成功している
- Render の最新 deploy が成功し、`/actuator/health` が healthy を返す
- `https://articleshelf.pages.dev` からログイン、記事一覧取得、記事追加などの主要導線が崩れていない
- 必要なら Render の cold start を考慮して、初回アクセス時の待機表示も確認する

### 9.4 タグ作成のタイミング

タグは「これが公開された版だ」と後から追えるために使う。
そのため、マージ前や deploy 前に先回りで切るより、公開確認が終わった commit に対して切るほうが運用しやすい。

推奨の流れ:

1. `main` へマージする
2. 自動デプロイ完了を確認する
3. 公開 URL で軽い smoke check をする
4. 問題なければ `vX.Y.Z` 形式などのタグを切る

タグ作成コマンド例:

```bash
git checkout main
git pull origin main
git tag -a v0.4.2 -m "Release v0.4.2"
git push origin v0.4.2
```

直前に `main` へマージされた最新 commit を公開版として扱うなら、上の流れで十分です。
特定 commit を明示してタグを付けたい場合は、commit SHA を指定できます。

```bash
git tag -a v0.4.2 <commit-sha> -m "Release v0.4.2"
git push origin v0.4.2
```

タグを付ける前に `git pull origin main` で手元の `main` を最新化しておくと、古い commit へ誤ってタグを打つ事故を避けやすい。

### 9.5 リリースノート作成の流れ

GitHub Release は、タグに対応する公開履歴の説明として使う。
`main` 反映前に先に書くより、反映後に実際に公開された内容でまとめるほうがずれにくい。

リリースノートには最低限次を入れる。

- 何が変わったか
- ユーザー影響がある変更点
- 運用上の注意点や既知の制約
- 必要なら migration、環境変数、手動対応の有無

書き方の目安:

- `Added`: 新機能
- `Changed`: 既存仕様の変更
- `Fixed`: 不具合修正
- `Notes`: 既知事項、補足、運用メモ

GitHub CLI を使う場合のコマンド例:

```bash
gh release create v0.4.2 --generate-notes --title "v0.4.2"
```

下書きから整えたい場合:

```bash
gh release create v0.4.2 --draft --generate-notes --title "v0.4.2"
```

`--generate-notes` を使うと GitHub の差分から草案を作れるので、そこに運用メモや注意点を追記する流れがやりやすい。

### 9.6 緊急修正時の流れ

- 修正ブランチで対応する
- PR を作成して CI を通す
- `main` へマージして自動デプロイする
- 公開確認後、必要なら patch version のタグと GitHub Release を追加する

ロールバックが必要な場合は、Cloudflare Pages / Render の dashboard から直前 deploy を再反映する。
ただし DB migration を含む変更では、アプリだけ戻しても整合しない可能性があるため、事前に影響を確認する。

## 10. 公式参照

- Cloudflare Pages build configuration: https://developers.cloudflare.com/pages/configuration/build-configuration/
- Cloudflare Pages redirects: https://developers.cloudflare.com/pages/configuration/redirects/
- Render Deploy for Free: https://render.com/docs/free
- Render Web Services: https://render.com/docs/web-services/
- Supabase pricing: https://supabase.com/pricing
- Supabase database size: https://supabase.com/docs/guides/platform/database-size
- Supabase connection strings: https://supabase.com/docs/reference/postgres/connection-strings
- Supabase SSL enforcement: https://supabase.com/docs/guides/platform/ssl-enforcement
