# Responsive Capture

サイズ別レスポンシブ診断スクリーンショットの実行コマンド、出力先、確認観点をまとめる。
公式デザインスクリーンショットの保存先、撮影コマンド、画面別キャプチャ一覧は [../screenshots/README.md](../screenshots/README.md) を正とする。

## 1. 出力先

レスポンシブ崩れ確認用のサイズ別スクリーンショットは、公式画像とは分けて `frontend/test-results/responsive-screenshots/<command>/<viewport>/` に出力する。
標準確認サイズは [README.md](README.md) の対象サイズに従う。

## 2. 実行コマンド

- `npm run capture:responsive`: `frontend/test-results/responsive-screenshots/capture-responsive-all/` に標準確認サイズをまとめて撮影する
- `npm run capture:responsive:laptop`: `frontend/test-results/responsive-screenshots/capture-responsive-laptop/` に `1366x768` を撮影する
- `npm run capture:responsive:macbook`: `frontend/test-results/responsive-screenshots/capture-responsive-macbook/` に `1440x900` を撮影する
- `npm run capture:responsive:tablet`: `frontend/test-results/responsive-screenshots/capture-responsive-tablet/` に `820x1180` を撮影する
- `npm run capture:responsive:mobile`: `frontend/test-results/responsive-screenshots/capture-responsive-mobile/` に `430x932`、`390x844`、`375x667` を撮影する

対象画面だけ確認する場合は、`ARTICLESHELF_SCREENSHOT_TARGET=list|detail|dialogs|calendar|tags|mobile|all` を指定する。
特定サイズだけ直接指定する場合は、`ARTICLESHELF_SCREENSHOT_VIEWPORT=desktop|macbook|laptop|tablet|mobile|mobile-md|mobile-sm` を使う。

## 3. 確認観点

- 主要操作が画面外に消えない
- 検索、並び替え、フィルタ、追加ボタンの文言が見切れない
- 記事カードのタイトル、概要、タグ、日付、ステータス、操作ボタンが重ならない
- 詳細の閲覧 / 編集切り替えで、保存、削除、戻る、メタ情報が見切れない
- スマホでボトムナビが一覧末尾、モーダル、確認操作を邪魔しない
- カレンダー、タグ管理、フィルタ、追加、削除確認がタブレット / スマホで操作できる
