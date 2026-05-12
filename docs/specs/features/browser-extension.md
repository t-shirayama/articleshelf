# Chrome 拡張機能仕様

## 位置づけ

Chrome 拡張機能は、現在開いている記事ページの URL とタイトルを ArticleShelf の記事追加モーダルへ渡す補助導線である。
記事の保存、OGP 取得、重複確認、認証、ユーザースコープ、URL 再検証は Web アプリと backend の既存仕様を正本とする。
要件は [Chrome 拡張機能](../../requirements/functional/browser-extension.md) を参照する。

## 拡張機能本体

- 拡張機能は Manifest V3 の popup 型として実装する
- manifest の権限は `activeTab` のみにする
- popup は現在アクティブなタブの `url` と `title` を取得する
- ArticleShelf 接続先 URL は配布物ごとに固定し、popup では変更できない
- 本番用の接続先 URL は `https://articleshelf.pages.dev`
- ローカル確認用の接続先 URL は `http://localhost:5173`
- 拡張機能は `chrome.storage` を使わない
- 拡張機能は access token、refresh token、cookie、CSRF token、ArticleShelf の記事データを保存しない

## ArticleShelf への引き渡し

- popup の主要操作は、新しいタブで ArticleShelf の記事一覧 route を開く
- Web app route は `/articles?source=extension&articleUrl=...&articleTitle=...` を使う
- `articleUrl` には現在タブの URL を設定する
- `articleTitle` には現在タブの title が空でない場合だけ設定する
- `http` / `https` 以外の現在タブでは、ArticleShelf への引き渡し操作を無効化する
- Chrome 内部ページ、拡張機能ページ、blank tab は対象外として扱う

## Web アプリ側の受け取り

- `/articles` は `source=extension`、`articleUrl`、`articleTitle` query を受け取った場合、記事追加モーダルを自動で開く
- `articleUrl` は Web アプリ側でも `http` / `https` URL として再検証する
- query から作った draft は一度だけ consume し、同じ URL / title の query でモーダルを重複起動しない
- URL と title は追加モーダルの初期値として扱い、利用者が保存前に編集できる
- preview API と保存 API は通常の記事追加と同じ検証、OGP 取得、重複 URL 判定を行う

## 認証復帰

- 未認証で拡張機能の route を開いた場合、router guard は `/login?returnTo=...` へ遷移する
- Login return route は `/login?returnTo=...` を使う
- `returnTo` は `/` で始まる app 内 path の場合だけ採用する
- ログイン成功後は `returnTo` の `/articles?source=extension...` へ戻り、追加モーダルを開く
- 認証済みで `/login` / `/register` を開いた場合は、通常仕様どおり `/articles` へ戻る

## 配布とバージョン表示

- 配布 asset 名は `articleshelf-chrome-extension.zip` とする
- ローカル確認用 asset 名は `articleshelf-chrome-extension-local.zip` とする
- 公式配布 URL の既定値は `https://github.com/t-shirayama/articleshelf/releases/latest/download/articleshelf-chrome-extension.zip`
- ローカル開発時の frontend 既定配布 URL は `/downloads/articleshelf-chrome-extension-local.zip` とする
- アカウント設定ダイアログには、配布 zip のダウンロードリンク、バージョン表示、ローカルインストール手順、再インストール案内を表示する
- frontend の配布 URL は `VITE_EXTENSION_DOWNLOAD_URL` で上書きできる
- frontend の表示バージョンは `VITE_EXTENSION_VERSION` で上書きできる
- `VITE_EXTENSION_VERSION` が未設定で既定の GitHub Releases URL を使う場合、frontend は GitHub Releases latest API から `tag_name` を取得できれば表示に反映する

## 開発者向けビルド

- 拡張機能の開発者向け手順は [chrome-extension README](../../../chrome-extension/README.md) を参照する
- `chrome-extension` で `npm run build` を実行すると、unpacked 配布物と zip を生成する
- build script は `dist/articleshelf-chrome-extension/`、`dist/articleshelf-chrome-extension.zip`、`dist/articleshelf-chrome-extension-local/`、`dist/articleshelf-chrome-extension-local.zip` を生成する
- ローカル確認用 zip は `frontend/public/downloads/articleshelf-chrome-extension-local.zip` に同期する
- Docker で生成する場合は repository root から `docker run --rm -v ${PWD}:/workspace -w /workspace/chrome-extension node:24-alpine sh -c "npm ci && npm run build"` を実行する
- 正式な利用者向け配布は GitHub Releases の zip を正とする
