# Chrome extension local distribution

## 状態

未対応

## 優先度

P2

## 目的

ArticleShelf へブラウザから記事を登録しやすくする Chrome 拡張機能を作成し、Chrome Web Store 登録料を避ける初期配布としてローカルインストール用 zip を提供する。
ユーザーが Web アプリから拡張機能をダウンロードし、Chrome のデベロッパーモードで読み込める状態にする。

## 対象

- `chrome-extension/`
- `chrome-extension/dist/`
- Chrome Extension manifest / popup / background または content script
- Web アプリ側のダウンロードリンク
- Web アプリ側のローカルインストール手順表示
- `docs/specs/features/README.md`
- `docs/specs/ui/README.md`
- `docs/architecture/frontend/README.md`
- `docs/testing/README.md`

## 対応内容

- `chrome-extension/` 配下に Chrome 拡張機能の実装、build、package 方針を置く。
- zip 配布物は `chrome-extension/dist/` に生成する前提にする。
- 拡張機能は現在開いているページの URL と title を ArticleShelf の記事追加導線へ渡せるようにする。
- 認証済み Web アプリとの連携方法を決める。初期版では token を拡張機能へ保存しない方針を優先し、Web アプリを開いて記事追加モーダルへ渡す方式を検討する。
- Web アプリに Chrome 拡張機能のダウンロードリンクを追加する。
- Web アプリに、Chrome の `chrome://extensions` でデベロッパーモードを有効化し、zip を展開して「パッケージ化されていない拡張機能を読み込む」手順を表示する。
- 配布 zip の更新、バージョン表示、ユーザーへの再インストール案内を整理する。
- Chrome Web Store 公開はこのタスクに含めず、将来候補として切り分ける。

## 完了条件

- `chrome-extension/` に拡張機能の実装とローカル開発手順がある。
- `chrome-extension/dist/` に配布 zip を生成できる。
- Web アプリ上に拡張機能のダウンロードリンクが表示される。
- Web アプリ上にローカルインストール手順が表示され、ユーザーが迷わず導入できる。
- 拡張機能から現在ページの URL / title を ArticleShelf の記事追加導線へ渡せる。
- token、cookie、CSRF、外部 URL 連携に関する security / testing 観点が docs に反映される。
- 主要ブラウザ導線の手動確認または E2E / integration に相当する検証手順が残っている。

## 根拠

Chrome Web Store での公開には登録料が必要なため、初期段階ではローカルインストール用の配布 zip を提供したい。
Web アプリ側にダウンロードリンクと導入手順を置くことで、ユーザーが README や別資料を探さずに拡張機能を導入できる。
