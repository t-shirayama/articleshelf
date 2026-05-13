# Chrome 拡張機能仕様

## 位置づけ

Chrome 拡張機能は、現在開いている記事ページを ArticleShelf へタブ遷移なしで直接保存する補助導線である。
要件は [Chrome 拡張機能](../../requirements/functional/browser-extension.md) を参照する。
旧互換として `/articles?source=extension&articleUrl=...&articleTitle=...` は維持するが、新しい拡張機能と利用者向け説明では PKCE direct save を正本とする。

## 拡張機能本体

- 拡張機能は Manifest V3 の popup 型として実装する
- manifest の権限は `activeTab`、`storage`、`identity` に限定する
- `identity` は `chrome.identity.launchWebAuthFlow` による初回認証にだけ使う
- host permissions は固定された ArticleShelf API origin のみにする
- `cookies`、`tabs`、`webRequest`、任意 origin への broad permission は追加しない
- popup は現在アクティブなタブの `url` と `title` を取得する
- ArticleShelf の app URL / API URL / extension ID / redirect URI は配布物ごとに固定し、popup では変更できない
- 本番用 app URL は `https://articleshelf.pages.dev`
- 本番用 API URL は `https://articleshelf.onrender.com`
- ローカル確認用 app URL は `http://localhost:5173`
- ローカル確認用 API URL は `http://localhost:8080`
- manifest には公開鍵 `key` を入れて extension ID を固定する。private key は Git に置かない
- 本番用とローカル確認用の extension ID は別々にする

## 認証

- 拡張機能は public client として扱い、Authorization Code + PKCE で認証する
- popup のログイン操作は `chrome.identity.launchWebAuthFlow` で `/extension/authorize` を開く
- Web アプリの `/extension/authorize` は通常の Web 認証を要求し、未認証なら `/login?returnTo=...` を経由する
- backend の `POST /api/extension/oauth/authorize` は認証済み Web ユーザーだけが呼べる
- PKCE は `S256` のみ許可する
- `state` と `code_verifier` は認証中だけ `chrome.storage.session` に保存する
- token exchange 成功後、拡張機能専用 access token と expiry を `chrome.storage.local` に保存する
- 拡張機能は password、session cookie、CSRF token、refresh token を扱わない

## 拡張機能 token

- token は opaque random token とし、DB には hash だけ保存する
- TTL は24時間
- refresh token は発行しない。期限切れ時は再ログインする
- scope は `article:lookup article:create article:update_status` に限定する
- token record は token id、user id、scope、expires_at、revoked_at、client id、extension id を保持する
- revoke は `revoked_at` で表す
- 既存 Web JWT とは混ぜず、extension API 専用 filter / auth principal で判定する
- extension token では通常 API、account API、tag mutation、delete、admin 操作を許可しない
- Web JWT では extension article API を利用できない

## Extension API

- `GET /api/extension/articles/lookup?url=...`
  - 現在 URL が登録済みか確認する
  - 認証ユーザーの `user_id` で必ずスコープする
- `POST /api/extension/articles`
  - 現在 URL を記事として追加する
  - request は `url`, `title`, `status`, `readDate`
  - `status` は `UNREAD` または `READ`
  - `READ` の場合は popup 押下日の local date を `YYYY-MM-DD` で送る
  - `UNREAD` の場合は `readDate: null` を送る
- `PATCH /api/extension/articles/{id}/status`
  - 登録済み記事の `status` と `readDate` だけを更新する
  - title、summary、favorite、rating、notes、tags は維持する
- extension API でも通常の記事 API と同じ URL 検証、OGP 補完、重複 URL 判定、ユーザースコープを適用する

## Popup UX

- `http` / `https` 以外の現在タブでは保存操作を無効化する
- Chrome 内部ページ、拡張機能ページ、blank tab は対象外として扱う
- manifest の名称、説明、action title は Chrome 拡張の `_locales` で日本語 / English を提供する
- popup は Chrome UI locale とブラウザ言語から `ja` / `en` を判定し、日本語以外は英語へフォールバックする
- extension API へのリクエストは popup locale に応じた `Accept-Language` を付与する
- popup は横幅を固定し、長い記事タイトルは2行で省略、URL とボタン文言は横方向に溢れないよう省略する
- Web app は拡張機能による裏登録や status 更新を、ArticleShelf タブへの `focus` または `visibilitychange` で前面復帰したときに記事一覧と集計用 snapshot を再取得して反映する
- 未ログイン時は「ArticleShelf にログイン」操作を表示する
- ログイン済みなら「未読で登録」「既読で登録」を表示する
- 処理中は両ボタンを disabled にし、登録中または更新中の状態を表示する
- 未登録 URL では押した操作に応じて記事を作成する
- 登録済み URL では現在状態を表示し、押した操作に応じて status だけ更新する
- token 期限切れまたは revoke 済みの場合は local token を削除し、再ログインを促す

## 互換 route

- Web app route `/articles?source=extension&articleUrl=...&articleTitle=...` は当面維持する
- `/articles` は `source=extension`、`articleUrl`、`articleTitle` query を受け取った場合、記事追加モーダルを自動で開く
- 未認証で互換 route を開いた場合は `/login?returnTo=...` へ遷移する
- 互換 route の URL / title は追加モーダルの初期値として扱い、利用者が保存前に編集できる

## 配布とバージョン表示

- 配布 asset 名は `articleshelf-chrome-extension.zip` とする
- ローカル確認用 asset 名は `articleshelf-chrome-extension-local.zip` とする
- 公式配布 URL の既定値は `https://github.com/t-shirayama/articleshelf/releases/latest/download/articleshelf-chrome-extension.zip`
- ローカル開発時の frontend 既定配布 URL は `/downloads/articleshelf-chrome-extension-local.zip` とする
- Chrome 拡張機能ダイアログには、配布 zip のダウンロードリンク、バージョン表示、インストール手順、再インストール案内を表示する
- frontend の配布 URL は `VITE_EXTENSION_DOWNLOAD_URL` で上書きできる
- frontend の表示バージョンは `VITE_EXTENSION_VERSION` で上書きできる
- `VITE_EXTENSION_VERSION` が未設定で既定の GitHub Releases URL を使う場合、frontend は GitHub Releases latest API から `tag_name` を取得できれば表示に反映する

## 開発者向けビルド

- 拡張機能の開発者向け手順は [chrome-extension README](../../../chrome-extension/README.md) を参照する
- `chrome-extension` で `npm run build` を実行すると、unpacked 配布物と zip を生成する
- build script は `dist/articleshelf-chrome-extension/`、`dist/articleshelf-chrome-extension.zip`、`dist/articleshelf-chrome-extension-local/`、`dist/articleshelf-chrome-extension-local.zip` を生成する
- ローカル確認用 zip は `frontend/public/downloads/articleshelf-chrome-extension-local.zip` に同期する
- Docker で生成する場合は repository root から `docker run --rm -v ${PWD}:/workspace -w /workspace/chrome-extension node:24-alpine sh -c "npm ci && npm run build"` を実行する
- 正式な利用者向け配布は GitHub Releases の本番用 zip を正とする
