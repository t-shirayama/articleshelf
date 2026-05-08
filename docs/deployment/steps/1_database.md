# Supabase 本番DB作成・DB接続情報確認手順

対象: ReadStack 本番環境
目的: Render backend から接続する Supabase PostgreSQL を作成し、Render に設定する DB 接続情報を確認する

---

## 1. Supabase で本番DBを作成

### 1.1 Supabase にログイン

1. Supabase にアクセスする
2. GitHub / Google / メールアドレスなどでログインする
3. 初回の場合はアカウント作成を完了する

---

### 1.2 Organization を作成または選択

1. Supabase Dashboard に入る
2. 既存 Organization があれば選択する
3. なければ新規 Organization を作成する

Organization 名の例:

```text
readstack
```

---

### 1.3 New project を作成

1. Dashboard で **New project** を選択する
2. 以下のように入力する

```text
Project name: readstack-prod
Database password: 強力なランダムパスワード
Region: Render backend に近いリージョン
Pricing plan: Free
```

おすすめ:

```text
Project name: readstack-prod
```

注意:

- Database password はあとで Render の環境変数に使う
- パスワードは必ずパスワードマネージャーに保存する
- 紛失すると再設定が必要になる
- Region は後から簡単に変えられない前提で選ぶ
- 日本からの利用が中心なら Asia 系リージョンを優先
- Render の backend リージョンと近い場所を選ぶと遅延が少ない

---

### 1.4 Project 作成完了を待つ

1. Supabase がプロジェクトを作成するまで待つ
2. Dashboard に入れるようになったら作成完了
3. 左メニューに以下が表示されることを確認する

```text
Table Editor
SQL Editor
Database
Project Settings
```

---

## 2. DB接続情報を確認

ReadStack backend は Spring Boot から PostgreSQL に接続するため、Supabase の **Postgres connection string** を確認する。

Supabase 公式ドキュメントでは、接続情報は Project Dashboard の **Connect** ボタンから確認できる。Render のような永続的な backend から接続する場合、IPv6 が使えるなら Direct connection、IPv4 互換が必要なら Pooler session mode が推奨される。

---

### 2.1 接続情報画面を開く

1. Supabase Dashboard で `readstack-prod` プロジェクトを開く
2. 画面上部または Database 周辺にある **Connect** を選択する
3. 接続方式の一覧を開く
4. Connection string を確認する

---

### 2.2 Render backend 用の接続方式を選ぶ

Render から Supabase に接続する場合は、まず以下を優先する。

```text
Pooler session mode
```

理由:

- Supabase の Direct connection は IPv6 前提の場合がある
- Pooler session mode は IPv4 / IPv6 の両方に対応しやすい
- Spring Boot のような永続 backend には transaction mode より session mode が無難

避けるもの:

```text
Transaction pooler mode
```

理由:

- transaction mode は serverless / edge function 向け
- prepared statement 非対応などで Java / Hibernate / HikariCP と相性問題が出る可能性がある

---

### 2.3 接続文字列をコピーする

Supabase の Pooler session mode では、おおよそ以下のような形式になる。

```text
postgres://postgres.<PROJECT_REF>:[YOUR-PASSWORD]@aws-0-<REGION>.pooler.supabase.com:5432/postgres
```

または Direct connection の場合は以下のような形式になる。

```text
postgresql://postgres:[YOUR-PASSWORD]@db.<PROJECT_REF>.supabase.co:5432/postgres
```

`[YOUR-PASSWORD]` は、Project 作成時に設定した Database password に置き換える。

---

### 2.4 Spring Boot 用 JDBC URL に変換する

Render の `SPRING_DATASOURCE_URL` には、`postgres://` ではなく JDBC 形式を設定する。

Pooler session mode の例:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-<REGION>.pooler.supabase.com:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.<PROJECT_REF>
SPRING_DATASOURCE_PASSWORD=<DATABASE_PASSWORD>
```

Direct connection の例:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://db.<PROJECT_REF>.supabase.co:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<DATABASE_PASSWORD>
```

ReadStack では、まず Pooler session mode を使う想定でよい。

---

## 3. Render に設定する値を整理

Render backend の Environment Variables に入れる DB 関連値は以下。

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-<REGION>.pooler.supabase.com:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.<PROJECT_REF>
SPRING_DATASOURCE_PASSWORD=<DATABASE_PASSWORD>
```

あわせて本番用に以下も設定する。

```env
SPRING_PROFILES_ACTIVE=prod
```

接続プールを小さめにする場合は以下も設定する。

```env
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=3
```

---

## 4. 接続情報の確認チェックリスト

以下が埋まっていれば DB 接続情報の確認は完了。

```text
[ ] Supabase project name: readstack-prod
[ ] Supabase region を確認した
[ ] Database password を保存した
[ ] Pooler session mode の host を確認した
[ ] PROJECT_REF を確認した
[ ] SPRING_DATASOURCE_URL を JDBC 形式に変換した
[ ] SPRING_DATASOURCE_USERNAME を確認した
[ ] SPRING_DATASOURCE_PASSWORD を確認した
[ ] sslmode=require を URL に付けた
```

---

## 5. 次の工程

DB 作成と接続情報確認が完了したら、次は Render backend の Environment Variables を設定する。

次工程:

```text
1. Render backend service を作成
2. Dockerfile path / root directory を設定
3. Supabase DB 接続情報を環境変数に設定
4. JWT / refresh token / CORS / CSRF の本番環境変数を設定
5. backend を初回デプロイ
6. Flyway migration が成功することを確認
7. /actuator/health を確認
```
