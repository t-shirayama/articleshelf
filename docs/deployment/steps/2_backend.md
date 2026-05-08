# Render backend デプロイ手順

対象: ReadStack backend
目的: Supabase 本番DBに接続する Render Web Service を作成し、backend 初回起動時に Flyway migration を実行して `/actuator/health` を確認する

---

## 前提

Supabase 側で以下が確認済みであること。

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-<REGION>.pooler.supabase.com:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.<PROJECT_REF>
SPRING_DATASOURCE_PASSWORD=<DATABASE_PASSWORD>
```

ReadStack backend は Spring Boot + Flyway 構成のため、backend 起動時に Flyway migration が実行される。

Render の Web Service は public URL を持つ動的アプリ向けサービスで、GitHub リポジトリまたは Docker image からデプロイできる。Dockerfile から build する場合、Render 側で Language を Docker にする。

参考:

- Render Web Services: https://render.com/docs/web-services/

---

## 3. Render backend に DB 接続環境変数を設定

### 3.1 Render アカウントを作成

1. Render にアクセスする
2. **Get Started** または **Sign up** を選択する
3. GitHub アカウントでサインアップする

おすすめ:

```text
GitHub アカウントでログイン
```

理由:

- GitHub repository との接続が簡単
- push / merge による Auto Deploy を使いやすい
- 後で GitHub Actions CD に移行しやすい

---

### 3.2 GitHub repository を接続

1. Render Dashboard に入る
2. **New +** を選択する
3. **Web Service** を選択する
4. Source として **Git Provider** を選択する
5. GitHub 連携を許可する
6. repository 一覧から以下を選択する

```text
t-shirayama/readstack
```

Render は GitHub / GitLab / Bitbucket などの repository と連携し、linked branch に push / merge された変更を自動デプロイできる。

---

### 3.3 backend Web Service の基本設定

Web Service 作成画面で以下を設定する。

```text
Name: readstack-backend
Region: Supabase DB に近いリージョン
Branch: develop または main
Root Directory: backend
Runtime / Language: Docker
Dockerfile Path: ./Dockerfile
Instance Type: Free または Starter
```

初回は、現在デプロイ対象として確認している branch を選ぶ。

```text
Branch: develop
```

本番運用を main に寄せる場合は、先に `develop` から `main` へ merge してから以下にする。

```text
Branch: main
```

注意:

- 今回の初回デプロイでは、GitHub Actions CD ではなく Render の手動デプロイまたは Auto Deploy でよい
- CD 化は、手動デプロイ成功後に行う
- 無料枠では初回アクセスやスリープ復帰が遅くなる可能性がある

---

### 3.4 Environment Variables を設定

Web Service 作成画面、または作成後の **Environment** で以下を設定する。

#### 必須: Spring profile

```env
SPRING_PROFILES_ACTIVE=prod
```

---

#### 必須: Supabase DB 接続情報

Supabase の Pooler session mode を使う場合:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-<REGION>.pooler.supabase.com:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.<PROJECT_REF>
SPRING_DATASOURCE_PASSWORD=<DATABASE_PASSWORD>
```

Direct connection を使う場合:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://db.<PROJECT_REF>.supabase.co:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<DATABASE_PASSWORD>
```

おすすめは Pooler session mode。

---

#### 必須: フロントエンド Origin

Cloudflare Pages の URL がまだ未確定の場合は、いったん仮の値を設定する。

```env
FRONTEND_ORIGIN=https://<your-cloudflare-pages-domain>.pages.dev
```

Cloudflare Pages の本番URLが確定したら、必ずこの値を更新する。

例:

```env
FRONTEND_ORIGIN=https://readstack.pages.dev
```

---

#### 必須: 認証 secret

以下は必ず本番用のランダム値にする。

```env
JWT_ACCESS_SECRET=<32文字以上のランダム文字列>
AUTH_REFRESH_TOKEN_HASH_SECRET=<32文字以上のランダム文字列>
```

生成例:

```bash
openssl rand -base64 48
```

注意:

- repository に commit しない
- README やメモに平文で残さない
- Render Environment Variables にのみ登録する
- dev 用 default secret は絶対に使わない

---

#### 必須: Cookie / CSRF

```env
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=None
AUTH_CSRF_ENABLED=true
```

本番では refresh / logout が cookie 認証ベースになるため、CSRF は有効にする。

---

#### 必須: 初期ユーザー作成を無効化

```env
READSTACK_INITIAL_USER_ENABLED=false
```

本番通常運用では初期ユーザーを自動作成しない。

---

#### 推奨: Rate limit

```env
READSTACK_AUTH_RATE_LIMIT_ENABLED=true
READSTACK_LOGIN_RATE_LIMIT_CAPACITY=5
READSTACK_LOGIN_RATE_LIMIT_WINDOW_SECONDS=60
READSTACK_REGISTER_RATE_LIMIT_CAPACITY=3
READSTACK_REGISTER_RATE_LIMIT_WINDOW_SECONDS=600
```

---

#### 推奨: DB connection pool を小さめにする

無料枠 / 小規模運用では DB 接続数を増やしすぎない。

```env
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=3
```

---

### 3.5 Render の PORT について

ReadStack backend は以下の設定になっている。

```yaml
server:
  port: ${PORT:8080}
```

Render は Web Service に `PORT` を設定でき、アプリは public internet から受けるために `0.0.0.0` の指定 port で待ち受ける必要がある。Render の Web Service では既定 port は `10000` で、`PORT` 環境変数も利用できる。

通常、Render 側で `PORT` は明示設定しなくてよい。
アプリ側が `${PORT:8080}` を見て起動するため、Render が渡す `PORT` に従う。

---

## 4. Render backend をデプロイ

### 4.1 Web Service を作成する

1. 設定内容を確認する
2. **Create Web Service** を選択する
3. Render の build / deploy が開始される
4. Logs を開く

---

### 4.2 build log を確認する

以下のような流れになっていることを確認する。

```text
Docker build started
Maven package started
readstack-backend-0.1.0.jar created
Docker image built
Service starting
```

ReadStack の `backend/Dockerfile` は Maven で jar を build し、最終 image で `java -jar app.jar` を実行する構成。

```text
FROM maven:... AS build
Docker build 内で package を実行
FROM eclipse-temurin:21-jre-alpine
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### 4.3 初回 deploy の成功条件

Render の deploy が成功すると、backend に以下のような URL が発行される。

```text
https://readstack-backend.onrender.com
```

この URL は後で Cloudflare Pages 側の `VITE_API_BASE_URL` に使う。

---

## 5. backend 起動時に Flyway migration が走る

### 5.1 Flyway migration log を確認

Render の Logs で `Flyway` を検索する。

期待するログ例:

```text
Flyway Community Edition ...
Database: jdbc:postgresql://...
Successfully validated ...
Creating Schema History table ...
Migrating schema ...
Successfully applied ... migration
```

または既に適用済みの場合:

```text
Schema is up to date. No migration necessary.
```

---

### 5.2 JPA validate が通ることを確認

ReadStack は JPA の `ddl-auto` が `validate` になっているため、Flyway migration 後の schema と Entity が一致しない場合、起動に失敗する。

期待する状態:

```text
Application started
Tomcat started
Started ReadStackApplication
```

失敗時によくある原因:

```text
SPRING_DATASOURCE_URL が間違っている
SPRING_DATASOURCE_USERNAME が間違っている
SPRING_DATASOURCE_PASSWORD が間違っている
sslmode=require がない
Supabase project がまだ起動中
Flyway migration が途中で失敗した
既存DBに古い schema が残っている
JPA validate で column / constraint 不一致
```

---

### 5.3 migration 失敗時の対応

#### DB 接続エラーの場合

Render Environment Variables を確認する。

```env
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

特に以下を確認する。

```text
[ ] JDBC URL が jdbc:postgresql:// で始まっている
[ ] URL 末尾に ?sslmode=require が付いている
[ ] Pooler の場合 username が postgres.<PROJECT_REF> になっている
[ ] password に記号が含まれる場合でも正しく保存されている
```

---

#### migration SQL エラーの場合

1. Render Logs の Flyway エラーを読む
2. Supabase SQL Editor で schema 状態を確認する
3. 新規DBなら、一度 database を作り直す方が早い場合がある
4. 本番データ投入済みなら、手動修正せず migration 方針を決める

初回の空DBなら、壊れた migration を無理に直すより DB を作り直して再デプロイした方が安全。

---

## 6. `/actuator/health` を確認

### 6.1 Render の Health Check Path を設定

Render Dashboard で backend service を開く。

1. **Settings** を開く
2. **Health Check Path** を探す
3. 以下を設定する

```text
/actuator/health
```

Render の HTTP health check は、指定 path に GET を送り、`2xx` または `3xx` が返れば成功と判定する。新 deploy では health check が通るまで新 instance への traffic 切り替えを待ち、一定時間内に成功しない場合は deploy を失敗扱いにする。

---

### 6.2 ブラウザで確認

以下にアクセスする。

```text
https://readstack-backend.onrender.com/actuator/health
```

期待するレスポンス:

```json
{
  "status": "UP"
}
```

`application.yml` では health endpoint が公開され、詳細は表示しない設定になっている。

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

---

### 6.3 curl で確認

ローカル terminal から確認する。

```bash
curl -i https://readstack-backend.onrender.com/actuator/health
```

期待する結果:

```text
HTTP/2 200
```

本文:

```json
{ "status": "UP" }
```

---

## デプロイ完了チェックリスト

```text
[ ] Render アカウントを作成した
[ ] GitHub repository t-shirayama/readstack を接続した
[ ] Web Service を作成した
[ ] Root Directory を backend にした
[ ] Language / Runtime を Docker にした
[ ] Dockerfile Path を ./Dockerfile にした
[ ] Branch を develop または main にした
[ ] SPRING_PROFILES_ACTIVE=prod を設定した
[ ] Supabase DB 接続環境変数を設定した
[ ] JWT_ACCESS_SECRET を本番用ランダム値にした
[ ] AUTH_REFRESH_TOKEN_HASH_SECRET を本番用ランダム値にした
[ ] AUTH_COOKIE_SECURE=true を設定した
[ ] AUTH_COOKIE_SAME_SITE=None を設定した
[ ] AUTH_CSRF_ENABLED=true を設定した
[ ] READSTACK_INITIAL_USER_ENABLED=false を設定した
[ ] backend deploy が成功した
[ ] Flyway migration 成功ログを確認した
[ ] JPA validate が通った
[ ] /actuator/health が UP を返した
[ ] backend URL を控えた
```

---

## 次の工程

Render backend が `UP` になったら、次は Cloudflare Pages frontend をデプロイする。

Cloudflare Pages に設定する API URL:

```env
VITE_API_BASE_URL=https://readstack-backend.onrender.com
```

Cloudflare Pages の URL が確定したら、Render backend 側の `FRONTEND_ORIGIN` をその URL に更新して、backend を再デプロイする。

最終的な流れ:

```text
1. Render backend deploy
2. /actuator/health 確認
3. Cloudflare Pages frontend deploy
4. Cloudflare Pages URL を確認
5. Render の FRONTEND_ORIGIN を Cloudflare Pages URL に更新
6. backend redeploy
7. 新規登録 / ログイン / 記事追加 / logout を smoke test
```
