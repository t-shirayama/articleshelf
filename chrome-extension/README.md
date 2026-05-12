# ArticleShelf Chrome Extension

ArticleShelf へ現在のページを渡す Chrome 拡張機能です。
Chrome Web Store には公開せず、GitHub Releases の配布 zip をダウンロードして Developer mode で読み込む前提で管理します。

要件は [Chrome 拡張機能要件](../docs/requirements/functional/browser-extension.md)、現在仕様は [Chrome 拡張機能仕様](../docs/specs/features/browser-extension.md) を正本とします。

## 役割

- 現在のタブの `url` と `title` を取得する
- ArticleShelf の `/articles?source=extension&articleUrl=...&articleTitle=...` を新しいタブで開く
- token や cookie は拡張機能へ保存しない
- ArticleShelf の接続先 URL は popup 内で変更し、`chrome.storage.sync` に保存する

## ディレクトリ

- `src/`: manifest、popup HTML / CSS / JS
- `scripts/build.mjs`: unpacked 配布物と zip を生成する build script
- `dist/articleshelf-chrome-extension/`: Chrome の「Load unpacked」で読み込むフォルダ
- `dist/articleshelf-chrome-extension.zip`: 配布用 zip

## ビルド

```bash
cd chrome-extension
npm run build
```

ビルドは OS 非依存で `dist/articleshelf-chrome-extension.zip` を生成します。
併せて開発便宜のため `frontend/public/downloads/articleshelf-chrome-extension.zip` へも同期しますが、正式導線は GitHub Releases です。

## ローカルインストール

1. `npm run build` で `dist/articleshelf-chrome-extension/` を生成する
2. Chrome で `chrome://extensions` を開く
3. 右上の Developer mode を有効化する
4. `Load unpacked` から `dist/articleshelf-chrome-extension/` を選ぶ
5. popup を開いて ArticleShelf URL を確認し、`Open draft in ArticleShelf` を使う
