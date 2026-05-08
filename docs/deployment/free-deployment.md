# 無料枠を中心にした公開・CI/CD構成案

最終更新: 2026-05-08

## 1. 目的

ReadStack を初期公開するため、無料枠を中心にしたデプロイ構成、準備事項、必要なコード改修、GitHub Actions CI/CD、休眠対策を整理する。
対象は個人利用、社内説明、ポートフォリオ、小規模検証であり、商用 SLA や高可用性を前提にしない。

## 2. 前提

- フロントエンド: Vue 3 + TypeScript + Vite
- バックエンド: Spring Boot + Java 21
- DB: PostgreSQL
- ローカル開発: Docker Compose
- API: REST
- 現在の frontend Dockerfile は Vite 開発サーバー用
- 現在の backend Dockerfile は本番 JAR 起動ステージを持つ
- 現在の CI は frontend build と backend package を実行済み

無料枠は、容量制限、休眠、起動遅延、バックアップ制限、利用条件変更がある。
初期公開では「無料で試せる構成」として扱い、継続運用や役員向けデモの安定性が必要になった段階で有料枠へ移行する。

## 3. 推奨構成

### 3.1 初期推奨

| レイヤー | 推奨 | 理由 |
| --- | --- | --- |
| フロントエンド | Cloudflare Pages / Vercel / Render Static Site | Vite の `dist` を静的配信でき、無料枠で始めやすい |
| バックエンド | Render Web Service | Spring Boot の Docker / Java アプリを公開しやすい |
| DB | Neon または Supabase PostgreSQL | Render Free PostgreSQL は期限付きのため、継続利用には不向き |
| CI | GitHub Actions | 既存 CI を拡張しやすい |
| CD | 最初は PaaS の Git 連携、次に GitHub Actions deploy hook | 初期運用を単純化できる |

### 3.2 構成図

```text
User Browser
  |
  | HTTPS
  v
Static Hosting (Cloudflare Pages / Vercel / Render Static Site)
  |
  | HTTPS REST API
  v
Backend Web Service (Render)
  |
  | JDBC over TLS
  v
Managed PostgreSQL (Neon / Supabase / Render Postgres)
```

## 4. 無料枠候補の比較

2026-05-07 時点で公式情報を確認した前提を記載する。無料枠は変更されるため、公開直前に各サービスの pricing / docs を再確認する。

| サービス | 用途 | 無料枠での利点 | 注意点 |
| --- | --- | --- | --- |
| Render Web Service | Spring Boot API | Docker / Git 連携しやすい | Free web service は 15 分 inbound traffic がないと spin down。再起動に待ち時間がある |
| Render Static Site | Frontend | 静的サイトを無料公開できる | outbound bandwidth / build pipeline minutes の制限対象 |
| Render Postgres | DB | Render 内で接続しやすい | Free database は期限付き。公式 docs では Free PostgreSQL が 30 日で expire する制約がある |
| Cloudflare Pages | Frontend | Free plan で月 500 builds、静的配信が強い | backend は別途必要 |
| Vercel Hobby | Frontend | Git 連携と CDN が簡単 | API backend は別途必要。Hobby の用途・上限を確認する |
| Supabase Free | DB | PostgreSQL、認証、管理画面が使える | Free plan の DB 容量や project 数、バックアップ制限を確認する |
| Neon Free | DB | PostgreSQL、scale to zero、無料枠が比較的扱いやすい | idle 時に compute が停止する。接続復帰を考慮する |

## 5. Render 休眠と health check

Render の Free web service は、15 分間 inbound HTTP request または WebSocket message がないと spin down する。
次のアクセスで自動復帰するが、起動に時間がかかるため、初回 API 呼び出しが遅くなる。

### 5.1 対応方針

- UI では API 初回接続が遅い場合に、サーバー起動中の可能性を示す
- バックエンドに軽量な health endpoint を用意する
- Render の health check path には `/actuator/health` または `/api/health` を設定する
- 無料枠で常時起動を維持する目的の定期 ping は、各サービス規約と無料枠の趣旨を確認してから採用する
- 役員向けデモ直前は、手動で一度アクセスして cold start を済ませる運用も用意する

### 5.2 10分間隔 health check 案

Render の休眠が 15 分無通信で発生するため、10 分間隔の health check は cold start 回避として技術的には有効である。
GitHub Actions の schedule は公式 docs 上、最短 5 分間隔で実行できるため、10 分間隔は設定可能である。

ただし、次の理由で本番の標準運用としては慎重に扱う。

- 無料枠の利用意図に反する可能性がある
- GitHub Actions の無料 minutes を消費する
- Render 側の Free instance hours / bandwidth / service-initiated traffic 制限に影響しうる
- schedule は厳密な時刻実行を保証するものではない

採用する場合は、初期デモ期間だけ有効にし、README または運用メモに目的と停止条件を書く。

### 5.3 GitHub Actions healthcheck 例

```yaml
name: Healthcheck

on:
  schedule:
    - cron: '*/10 * * * *'
  workflow_dispatch:

jobs:
  ping:
    runs-on: ubuntu-latest
    steps:
      - name: Ping backend health
        run: curl -fsS "$BACKEND_HEALTH_URL"
        env:
          BACKEND_HEALTH_URL: ${{ secrets.BACKEND_HEALTH_URL }}
```

`BACKEND_HEALTH_URL` 例:

```text
https://readstack-api.onrender.com/actuator/health
```

## 6. 必要なコード改修

### 6.1 バックエンド

| 優先度 | 改修 | 理由 |
| --- | --- | --- |
| P0 | `server.port: ${PORT:8080}` にする | PaaS が `PORT` を指定する場合に対応 |
| P0 | Actuator health endpoint を本番運用向けに調整 | Render health check / 外部監視に必要 |
| P0 | CORS を本番 frontend origin に限定 | 公開環境で安全に API 通信する |
| P0 | DB SSL 接続の確認 | managed PostgreSQL が TLS を要求する場合に必要 |
| P0 | `ddl-auto=update` から migration へ移行検討 | 本番 DB の予期せぬ schema 変更を避ける |
| P1 | OGP 取得の timeout / User-Agent / SSRF 対策 | 公開 API から外部 URL を取得するため |
| P1 | 本番 profile の logging 調整 | SQL や secret を出しすぎない |
| P1 | graceful shutdown | deploy 時の中断を減らす |

`application.yml` の変更案:

```yaml
server:
  port: ${PORT:8080}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
```

### 6.2 フロントエンド

| 優先度 | 改修 | 理由 |
| --- | --- | --- |
| P0 | `VITE_API_BASE_URL` を公開 API URL に設定 | build 時に API 先を固定する |
| P0 | API 初回遅延の表示 | Render cold start 時の体験を悪化させない |
| P0 | production build の確認 | 静的 hosting へ `dist` を配置する |
| P1 | runtime config 方式の検討 | 環境ごとに rebuild せず API URL を切り替えたい場合 |
| P1 | 認証追加後の cookie / CORS credential 対応 | refresh token cookie を使う場合に必要 |

### 6.3 Docker

- frontend は本番では Docker を使わず、`npm ci && npm run build` の成果物を静的 hosting へ置く
- 現在の `frontend/Dockerfile` は開発サーバー用として扱う
- backend は既存 `backend/Dockerfile` の production stage を利用できる
- Render で Docker deploy する場合、build context は `backend` にする

## 7. 環境変数

### 7.1 Frontend

| 変数 | 例 | 説明 |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `https://readstack-api.onrender.com` | 公開 API URL |

### 7.2 Backend

| 変数 | 例 | 説明 |
| --- | --- | --- |
| `PORT` | `8080` | PaaS が指定する listen port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://.../readstack?sslmode=require` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `readstack` | DB ユーザー |
| `SPRING_DATASOURCE_PASSWORD` | `********` | DB パスワード |
| `FRONTEND_ORIGIN` | `https://readstack.pages.dev` | CORS 許可 origin |
| `JWT_ACCESS_SECRET` | `********` | 認証追加後の JWT 署名鍵 |
| `AUTH_REFRESH_TOKEN_HASH_SECRET` | `********` | refresh token HMAC 署名用 secret |
| `AUTH_CSRF_ENABLED` | `true` | 本番必須。`prod` profile では `false` を指定すると起動エラー |
| `AUTH_COOKIE_SECURE` | `true` | HTTPS cookie 必須。`SameSite=None` の場合も必須 |
| `AUTH_COOKIE_SAME_SITE` | `None` | frontend と API が別 site の場合。same-site 配信なら `Lax` も検討可 |
| `READSTACK_INITIAL_USER_ENABLED` | `false` | 初期 ADMIN の自動作成。通常は `false`、検証環境で必要な場合のみ `true` |
| `READSTACK_INITIAL_USERNAME` | `owner` | 初期 ADMIN を有効化した場合の username |
| `READSTACK_INITIAL_USER_PASSWORD` | `********` | 初期 ADMIN を有効化した場合の password |
| `SPRING_PROFILES_ACTIVE` | `prod` | 本番 profile |

秘密情報は Git にコミットしない。
GitHub Actions Secrets、Render Environment Variables、各 hosting provider の環境変数に登録する。

本番必須の認証設定:

```text
AUTH_CSRF_ENABLED=true
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=None
FRONTEND_ORIGIN=https://your-frontend.example.com
JWT_ACCESS_SECRET=<long-random-secret>
AUTH_REFRESH_TOKEN_HASH_SECRET=<long-random-secret>
```

`AUTH_COOKIE_SAME_SITE=None` は frontend と API が別 site の場合に使う。
同一 site 配信に寄せる場合は `Lax` も選べるが、`prod` profile では refresh / logout の cookie 認証を守るため CSRF は常に有効にする。

## 8. GitHub Actions CI/CD

### 8.1 現在の CI

現在の `.github/workflows/ci.yml` は次を実行する。

- frontend: `npm ci`, `npm run build`
- backend: GitHub Actions runner 上で backend package job を実行

### 8.2 目標

- PR では build / test を実行し、壊れた変更を main に入れない
- main push では CI 成功後に frontend / backend を deploy する
- deploy hook や API token は GitHub Secrets で管理する
- DB migration 導入後は、deploy 前後の migration 手順を明示する
- 失敗時は deploy しない

### 8.3 推奨 workflow 分割

| workflow | trigger | 内容 |
| --- | --- | --- |
| `ci.yml` | push, pull_request | frontend build, backend build/test |
| `e2e.yml` | pull_request, main push, manual | Docker Compose 起動、Playwright smoke |
| `deploy-frontend.yml` | main push | 静的 hosting へ frontend deploy |
| `deploy-backend.yml` | main push | Render deploy hook または provider API で backend deploy |
| `healthcheck.yml` | schedule, manual | 公開 backend の health check |

### 8.4 Render deploy hook 例

Render の deploy hook URL を repository secret `RENDER_DEPLOY_HOOK_URL` に入れる。

```yaml
name: Deploy Backend

on:
  workflow_run:
    workflows: ["CI"]
    types: [completed]
    branches: [main]

jobs:
  deploy:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest
    steps:
      - name: Trigger Render deploy
        run: curl -fsS -X POST "$RENDER_DEPLOY_HOOK_URL"
        env:
          RENDER_DEPLOY_HOOK_URL: ${{ secrets.RENDER_DEPLOY_HOOK_URL }}
```

PaaS 側の Git auto deploy を使う場合、この workflow は不要。
初期運用では「CI は GitHub Actions、deploy は PaaS Git 連携」にして、安定後に deploy hook へ寄せる。

### 8.5 Frontend deploy

Cloudflare Pages / Vercel / Render Static Site のいずれでも、基本設定は次の通り。

```text
Root directory: frontend
Build command: npm ci && npm run build
Output directory: dist
Environment variable:
  VITE_API_BASE_URL=https://readstack-api.example.com
```

## 9. DB 運用

### 9.1 初期公開

- managed PostgreSQL を使う
- DB の無料枠容量、停止条件、バックアップ有無、接続数を確認する
- 初期公開前に export 手順を用意する
- `ddl-auto=update` は検証環境までにし、本番前に migration 導入を優先する

### 9.2 Backup / Export

無料枠では自動 backup がない、または制限されることがある。
最低限、次を用意する。

- provider dashboard からの manual backup / export 手順
- `pg_dump` を使う手順
- 復元手順
- DB provider 変更時の移行手順

## 10. 公開準備チェックリスト

### 10.1 アプリ

- [ ] frontend build が成功する
- [ ] backend package / test が成功する
- [ ] 公開 frontend から API に通信できる
- [ ] 記事追加、一覧、詳細、編集、削除が動く
- [ ] OGP 取得が公開環境から動く
- [ ] API 初回遅延時の表示が破綻しない
- [ ] Markdown 安全境界が維持されている

### 10.2 インフラ

- [ ] DB 接続情報を環境変数に登録した
- [ ] `FRONTEND_ORIGIN` を公開 URL に設定した
- [ ] `AUTH_CSRF_ENABLED=true` を設定した
- [ ] `AUTH_COOKIE_SECURE=true` を設定した
- [ ] frontend と API が別 site の場合は `AUTH_COOKIE_SAME_SITE=None` を設定した
- [ ] `JWT_ACCESS_SECRET` と `AUTH_REFRESH_TOKEN_HASH_SECRET` に十分長いランダム値を設定した
- [x] health check endpoint を用意した
- [ ] Render health check path を設定した
- [ ] 無料枠の期限、容量、休眠条件を確認した
- [ ] backup / export 手順を確認した
- [ ] custom domain / HTTPS の必要性を判断した

### 10.3 CI/CD

- [ ] PR で CI が実行される
- [ ] main push で CI が実行される
- [ ] CI 失敗時に deploy されない
- [ ] deploy hook は secret に保存している
- [ ] healthcheck workflow の目的と停止条件を記載した

## 11. 公式参照

- Render Deploy for Free: https://render.com/docs/free
- Render Health Checks: https://render.com/docs/health-checks
- Render Deploy Hooks: https://render.com/docs/deploy-hooks
- GitHub Actions workflow syntax: https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax
- GitHub Actions billing and usage: https://docs.github.com/actions/learn-github-actions/usage-limits-billing-and-administration
- Cloudflare Pages limits: https://developers.cloudflare.com/pages/platform/limits/
- Supabase billing / Free Plan: https://supabase.com/docs/guides/platform/billing-on-supabase
- Neon plans: https://neon.com/docs/introduction/pro-plan
- Vercel pricing: https://vercel.com/pricing

## 12. 未決事項

- 初期公開 DB を Neon、Supabase、Render Postgres のどれにするか
- 10 分間隔 health check を常時運用するか、デモ期間限定にするか
- custom domain を初期から設定するか
- 認証追加後、frontend / backend を same-site に寄せるか
- migration 導入を公開前必須にするか
