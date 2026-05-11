# ArticleShelf Chrome Extension

Chrome 拡張機能の実装を置く予定のディレクトリです。

## 方針

- 初期配布は Chrome Web Store ではなく、ローカルインストール用 zip として扱います。
- 配布 zip は `chrome-extension/dist/` に生成します。
- Web アプリ側にはダウンロードリンクと、Chrome のデベロッパーモードで読み込む手順を表示します。

## 出力先

`dist/` は配布物の出力先です。実装時に package / build script を追加し、zip 生成手順をここへ追記します。
