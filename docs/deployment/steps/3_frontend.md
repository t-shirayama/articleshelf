# Cloudflare Pages frontend デプロイ手順

対象: ArticleShelf frontend
目的: Cloudflare Pages に Vue / Vite frontend をデプロイし、Render backend と接続する

---

## 前提

Render backend がデプロイ済みで、以下の URL が確認できていること。

```text
https://articleshelf-backend.onrender.com
```

Render backend の health check が成功していること。

```text
https://articleshelf-backend.onrender.com/actuator/health
```

期待するレスポンス:

```json
{ "status": "UP" }
```

Cloudflare Pages は GitHub repository と連携すると、push ごとに自動 build / deploy できる。GitHub integration では preview deployment や GitHub 上の check run も利用できる。([developers.cloudflare.com](https://developers.cloudflare.com/pages/configuration/git-integration/github-integration/))

---

## 7. Cloudflare Pages frontend をデプロイ

## 7.1 Cloudflare アカウントを作成

1. Cloudflare にアクセスする
2. **Sign Up** を選択する
3. メールアドレスとパスワードでアカウントを作成する
4. メール認証を完了する
5. Cloudflare Dashboard にログインする

---

## 7.2 Workers & Pages を開く

1. Cloudflare Dashboard に入る
2. 左メニューから **Workers & Pages** を選択する
3. **Create application** を選択する
4. **Pages** を選択する
5. **Connect to Git** または **Import an existing Git repository** を選択する

Cloudflare Pages の Git integration は GitHub / GitLab に対応しており、repository への push を検知して自動 build / deploy できる。([developers.cloudflare.com](https://developers.cloudflare.com/pages/get-started/git-integration/))

---

## 7.3 GitHub repository を接続

1. Git provider として **GitHub** を選択する
2. Cloudflare Workers and Pages GitHub App の連携を許可する
3. repository access は必要最小限にする

おすすめ:

```text
Only select repositories
```

選択する repository:

```text
t-shirayama/articleshelf
```

4. repository を選択する
5. **Begin setup** を選択する

---

## 7.4 Pages project の基本設定

以下のように設定する。

```text
Project name: articleshelf
Production branch: develop または main
Framework preset: Vite
Root directory: frontend
Build command: npm run build
Build output directory: dist
```

ArticleShelf の frontend は Vite 構成なので、Cloudflare Pages では build command を `npm run build`、build output directory を `dist` にする。Cloudflare の Vite guide でもこの組み合わせが案内されている。([developers.cloudflare.com](https://developers.cloudflare.com/pages/framework-guides/deploy-a-vite3-project/))

---

## 7.5 Production branch を選ぶ

現在 Render backend と同じ branch を使うなら以下。

```text
Production branch: develop
```

本番運用を `main` に寄せるなら、先に `develop` から `main` へ merge してから以下。

```text
Production branch: main
```

おすすめの運用:

```text
develop: preview / staging 用
main: production 用
```

初回は、backend と frontend の branch がズレないようにすることを優先する。

```text
Render backend branch = Cloudflare Pages production branch
```

---

## 7.6 Build settings を入力

Cloudflare Pages の build 設定に以下を入力する。

```text
Framework preset: Vite
Build command: npm run build
Build output directory: dist
Root directory: frontend
```

もし Root directory を `frontend` にした場合、build output directory は `frontend/dist` ではなく、通常は以下でよい。

```text
dist
```

理由:

```text
Root directory が frontend なので、Cloudflare の build 実行場所は frontend 配下になるため。
```

---

## 7.7 Environment variables を設定

Pages project 作成画面、または作成後の **Settings > Environment variables** で設定する。

Production 用:

```env
VITE_API_BASE_URL=https://articleshelf-backend.onrender.com
```

Preview 用も使う場合:

```env
VITE_API_BASE_URL=https://articleshelf-backend.onrender.com
```

Cloudflare Pages では、build 時に必要な環境変数を Pages project の Settings から設定できる。`CF_PAGES`, `CF_PAGES_BRANCH`, `CF_PAGES_URL` などの system environment variables も自動注入される。([developers.cloudflare.com](https://developers.cloudflare.com/pages/configuration/build-configuration/))

注意:

- Vite で frontend に埋め込む環境変数は `VITE_` prefix が必要
- `VITE_API_BASE_URL` は browser に公開される値なので secret ではない
- JWT secret や DB password は絶対に Cloudflare Pages に設定しない
- secret 系は Render backend の Environment Variables にだけ設定する

---

## 7.8 Save and Deploy

1. 設定内容を確認する
2. **Save and Deploy** を選択する
3. Cloudflare Pages の build が開始される
4. build log を確認する

期待する流れ:

```text
Installing dependencies
npm ci
npm run build
vite build
dist generated
Deploying to Cloudflare Pages
Deployment successful
```

---

## 7.9 デプロイURLを確認

デプロイが成功すると、Cloudflare Pages の URL が発行される。

例:

```text
https://articleshelf.pages.dev
```

または project name に応じて以下のようになる。

```text
https://<project-name>.pages.dev
```

この URL を控える。

```text
Cloudflare Pages URL: https://articleshelf.pages.dev
```

---

## 7.10 Render backend の FRONTEND_ORIGIN を更新

Cloudflare Pages の URL が確定したら、Render backend 側の環境変数を更新する。

Render Dashboard で backend service を開く。

1. `articleshelf-backend` service を開く
2. **Environment** を開く
3. 以下を更新する

```env
FRONTEND_ORIGIN=https://articleshelf.pages.dev
```

4. 保存する
5. backend を再デプロイする

理由:

- frontend と backend は別 origin になる
- backend 側の CORS / cookie / CSRF 設定で frontend origin が必要
- `FRONTEND_ORIGIN` が仮値のままだと、browser から API 通信が失敗する可能性がある

---

## 7.11 再デプロイ後に backend health を確認

Render backend の再デプロイ後に確認する。

```bash
curl -i https://articleshelf-backend.onrender.com/actuator/health
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

## 7.12 frontend の表示確認

Cloudflare Pages URL にアクセスする。

```text
https://articleshelf.pages.dev
```

確認項目:

```text
[ ] 画面が表示される
[ ] Console に build error が出ていない
[ ] API 通信が Render backend に向いている
[ ] CORS error が出ていない
[ ] ログイン画面または初期画面が表示される
```

---

## 7.13 Smoke test

本番 frontend から以下を確認する。

```text
[ ] 新規登録できる
[ ] ログインできる
[ ] ログイン状態で /api/users/me が成功する
[ ] 記事を追加できる
[ ] 記事一覧が表示される
[ ] 記事を編集できる
[ ] 記事を削除できる
[ ] タグを追加できる
[ ] OGP取得が動く
[ ] ログアウトできる
[ ] 再ログイン後に記事が残っている
```

---

## 7.14 よくあるエラーと対応

### CORS error が出る

症状:

```text
Access to fetch at ... has been blocked by CORS policy
```

確認する値:

```env
FRONTEND_ORIGIN=https://articleshelf.pages.dev
VITE_API_BASE_URL=https://articleshelf-backend.onrender.com
```

対応:

```text
1. Render の FRONTEND_ORIGIN を Cloudflare Pages の実URLにする
2. Render backend を再デプロイする
3. ブラウザを hard reload する
```

---

### API URL が localhost のまま

症状:

```text
http://localhost:8080 に API リクエストしている
```

原因:

```text
VITE_API_BASE_URL が Cloudflare Pages に設定されていない
```

対応:

```text
1. Cloudflare Pages > Settings > Environment variables を開く
2. VITE_API_BASE_URL を設定する
3. frontend を再デプロイする
```

---

### Cookie / refresh / logout が失敗する

確認する値:

Render backend:

```env
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=None
AUTH_CSRF_ENABLED=true
FRONTEND_ORIGIN=https://articleshelf.pages.dev
```

Cloudflare Pages:

```env
VITE_API_BASE_URL=https://articleshelf-backend.onrender.com
```

対応:

```text
1. Render backend の環境変数を修正する
2. backend を再デプロイする
3. frontend を hard reload する
4. 再ログインする
```

---

### Cloudflare Pages で 404 になる

確認する値:

```text
Build output directory: dist
Root directory: frontend
```

原因候補:

```text
dist が生成されていない
Build output directory が frontend/dist になっている
index.html が dist 直下にない
```

Cloudflare Pages は build output directory の中にある `index.html` を配信するため、SPA の top-level `index.html` が必要。([developers.cloudflare.com](https://developers.cloudflare.com/pages/framework-guides/deploy-anything/))

---

### Build が Node version で失敗する

ArticleShelf frontend の `package.json` は Node 22 系を想定している。

Cloudflare Pages の environment variables に以下を追加する。

```env
NODE_VERSION=22
```

その後、再デプロイする。

---

## デプロイ完了チェックリスト

```text
[ ] Cloudflare アカウントを作成した
[ ] Workers & Pages を開いた
[ ] GitHub repository t-shirayama/articleshelf を接続した
[ ] repository access を必要最小限にした
[ ] Pages project を作成した
[ ] Project name を articleshelf にした
[ ] Production branch を backend と同じ branch にした
[ ] Framework preset を Vite にした
[ ] Root directory を frontend にした
[ ] Build command を npm run build にした
[ ] Build output directory を dist にした
[ ] VITE_API_BASE_URL を Render backend URL にした
[ ] Save and Deploy を実行した
[ ] Cloudflare Pages URL を確認した
[ ] Render backend の FRONTEND_ORIGIN を Cloudflare Pages URL に更新した
[ ] Render backend を再デプロイした
[ ] /actuator/health が UP を返した
[ ] frontend 画面が表示された
[ ] 新規登録 / ログイン / 記事追加 / ログアウトが成功した
```

---

## 次の工程

Cloudflare Pages frontend が正常に動いたら、最後に本番 smoke test と README 更新を行う。

次工程:

```text
1. 本番 smoke test
2. README に公開URLを追記
3. 無料枠の注意書きを追記
4. v0.1.0 tag を作成
5. GitHub Releases を作成
6. GitHub Actions CD を workflow_dispatch から検討
```

公開URLメモ:

```text
Frontend: https://articleshelf.pages.dev
Backend:  https://articleshelf-backend.onrender.com
Health:   https://articleshelf-backend.onrender.com/actuator/health
```
