# ArticleShelf Chrome Extension

ArticleShelf へ現在のページを直接保存する Chrome 拡張機能です。
Chrome Web Store には公開せず、GitHub Releases の配布 zip をダウンロードして Developer mode で読み込む前提で管理します。

要件は [Chrome 拡張機能要件](../docs/requirements/functional/browser-extension.md)、現在仕様は [Chrome 拡張機能仕様](../docs/specs/features/browser-extension.md) を正本とします。

## 役割

- 現在のタブの `url` と `title` を取得する
- 初回ログインで Authorization Code + PKCE により拡張機能専用 token を取得する
- popup から未読登録、既読登録、登録済み記事の既読/未読更新を行う
- token は保存用 scope の短命 opaque token とし、password、cookie、CSRF token、refresh token は扱わない
- ArticleShelf の app URL / API URL は配布物ごとに固定し、popup から変更できない

## ディレクトリ

- `src/`: manifest、popup HTML / CSS / JS
- `scripts/build.mjs`: unpacked 配布物と zip を生成する build script
- `dist/articleshelf-chrome-extension/`: 本番用 unpacked 配布物
- `dist/articleshelf-chrome-extension.zip`: 本番用 zip
- `dist/articleshelf-chrome-extension-local/`: ローカル確認用 unpacked 配布物
- `dist/articleshelf-chrome-extension-local.zip`: ローカル確認用 zip

## ビルド

```bash
cd chrome-extension
npm run build
```

ビルドは OS 非依存で本番用とローカル確認用の zip を生成します。
併せて開発便宜のため `frontend/public/downloads/articleshelf-chrome-extension-local.zip` へローカル確認用 zip を同期します。
正式な本番配布導線は GitHub Releases の `articleshelf-chrome-extension.zip` です。

Docker だけで生成する場合は、repository root から次を実行します。

```powershell
docker run --rm -v ${PWD}:/workspace -w /workspace/chrome-extension node:24-alpine sh -c "npm ci && npm run build"
```

## ローカルインストール

1. 上記の build で `dist/articleshelf-chrome-extension-local/` と `frontend/public/downloads/articleshelf-chrome-extension-local.zip` を生成する
2. Chrome で `chrome://extensions` を開く
3. 右上の Developer mode を有効化する
4. `Load unpacked` から `dist/articleshelf-chrome-extension-local/` を選ぶ
5. 通常の `http` / `https` ページで popup を開き、初回は `Log in to ArticleShelf` で認証する
6. `Save as unread` または `Save as read` で現在ページを登録する

ローカルの ArticleShelf 画面から zip をダウンロードする場合は、frontend を `http://localhost:5173` で起動したあと、`/downloads/articleshelf-chrome-extension-local.zip` を取得します。

ローカル確認用 artifact は ArticleShelf app URL を `http://localhost:5173`、API URL を `http://localhost:8080` に固定します。本番用 artifact は `https://articleshelf.pages.dev` と `https://articleshelf.onrender.com` に固定します。
