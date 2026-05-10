# Design Screenshots

ArticleShelf の現行画面キャプチャの保存先、撮影手順、画面別一覧をまとめる。
サイズ別のレスポンシブ診断画像は公式スクリーンショットとは分け、詳細は [../responsive/capture.md](../responsive/capture.md) に従う。

## 1. 保存先と撮影方針

`docs/designs/screenshots/` には、現行実装から取得した代表スクリーンショットとこの README を格納しています。
スクリーンショットは撮影コマンドごとのフォルダに分け、同じコマンド内では viewport サイズ別に管理します。
README の画面イメージに使う公式キャプチャは `docs/designs/screenshots/capture-designs/1920x1080/` 配下の画像だけを使い、ブラウザ locale は `ja-JP` で取得します。
キャプチャスクリプトは専用ユーザーと記事/タグを作成してから撮影し、既存ログイン状態や手元データへ依存しないようにします。

更新時は `docker compose -f docker-compose.e2e.yml up --build -d` でアプリを起動し、`frontend` で `npm run capture:designs` を実行します。
UI 仕様との差分が出た場合は、関連する design docs も同じ作業内で更新します。
UI に影響するコード変更を行った場合は、スクリーンショット更新を後続タスクにせず、同じ作業内で `docs/designs/screenshots/` を撮り直して説明文と同期します。
README に掲載する記事詳細ビューは、閲覧モードと編集モードをそれぞれ撮影し、モード差分が分かるようにします。
一部の追加キャプチャだけが必要で他の画像を更新しない場合は、撮影スクリプトの対象指定を使い、既存画像を再生成しないようにします。

実行コマンド:

- `npm run capture:designs`: `docs/designs/screenshots/capture-designs/1920x1080/` に公式デザインスクリーンショットを更新する
- `ARTICLESHELF_SCREENSHOT_TARGET=<target> npm run capture:designs`: 対象画面だけを撮影する

## 2. 公式キャプチャ一覧

- `screenshots/capture-designs/1920x1080/auth_login.png`: ログイン画面
  - ブラウザ自動入力とラベルが重ならない固定ラベル形式
  - ログイン / 登録の切り替えと主要アクション

- `screenshots/capture-designs/1920x1080/auth_register.png`: ユーザー登録画面
  - ユーザー名、表示名、パスワードの入力欄を表示
  - 表示名は任意であることをラベルとヒントで示す
  - プレースホルダーは入力例、入力ルールは欄下ヒントとして表示

- `screenshots/capture-designs/1920x1080/account_settings_dialog.png`: アカウント設定モーダル
  - 左サイドバー下部のアカウントボタンから開く
  - パスワード変更、全端末ログアウト、退会の操作領域を表示
  - 破壊的な退会操作を通常操作と区別して表示

- `screenshots/capture-designs/1920x1080/desktop_article_list.png`: デスクトップ版 記事一覧画面
  - 左サイドバーで未読 / 既読 / お気に入りなどの状態を選択
  - 上部のフィルタボタンからタグ、おすすめ度、日付条件を絞り込む
  - 左サイドバー下部に学習継続を後押しする画像付きメッセージを表示
  - メッセージは固定せず、アクションに合わせてローテーション表示
  - 中央にカード形式で記事を表示
  - 各カードにサムネイル、タイトル、タグ、ステータス、お気に入り操作を表示

- `screenshots/capture-designs/1920x1080/desktop_article_detail_view.png`: デスクトップ版 記事詳細画面 / 閲覧
  - 現在の実装はクリーム系のライトテーマで統一
  - タイトル、URL、タグを上部に配置
  - 記事イメージ、概要を中央に表示
  - 右側に既読日、登録日などのメタ情報
  - メモを下部に Markdown 表示

- `screenshots/capture-designs/1920x1080/desktop_article_detail_edit.png`: デスクトップ版 記事詳細画面 / 編集
  - 「記事の詳細」アコーディオンをメモ入力の上に置き、基本情報を確認してから追記しやすい編集フォームを表示
  - 閲覧 / 編集トグル、保存、削除、お気に入り操作を同じヘッダーに配置
  - 右側のステータス、既読日、おすすめ度を編集可能な固定スロットとして表示
  - 「記事の詳細」アコーディオンでタイトル、URL、概要、タグ編集をまとめる

- `screenshots/capture-designs/1920x1080/calendar_view.png`: カレンダー画面
  - 登録日 / 既読日の表示モード切り替え
  - 月移動、月内サマリー、日付ごとの記事表示

- `screenshots/capture-designs/1920x1080/tag_management.png`: タグ管理画面
  - タグ名検索、並び替え、タグ追加ボタンをヘッダー行に配置
  - タグ名、記事数、編集、統合、削除の操作を一覧で表示

- `screenshots/capture-designs/1920x1080/add_article_modal.png`: 記事追加モーダル
  - URL、タイトル、タグ、あとで読む、既読日、メモの入力フィールド
  - タグは選択済みタグ、既存タグからの追加、新規タグ入力に対応
  - 閉じる・保存ボタン

- `screenshots/capture-designs/1920x1080/filter_dialog.png`: フィルタモーダル
  - タグ、おすすめ度、登録日範囲、既読日範囲をまとめて調整
  - 条件クリア、閉じる、適用する操作をヘッダー行に配置

- `screenshots/capture-designs/1920x1080/tag_add_dialog.png`: タグ追加モーダル
  - タグ名を単体で追加するフォーム
  - 入力欄と追加ボタンの右端を揃える

- `screenshots/capture-designs/1920x1080/tag_merge_dialog.png`: タグ統合モーダル
  - 統合元タグと統合先セレクトを表示
  - 統合後の記事紐づけ移動を確認して実行

- `screenshots/capture-designs/1920x1080/tag_delete_dialog.png`: タグ削除確認ダイアログ
  - 未使用タグの削除対象を確認する
  - 記事自体は削除されないことを文言で補足する

- `screenshots/capture-designs/1920x1080/delete_article_dialog.png`: 記事削除確認ダイアログ
  - 削除対象を確認し、キャンセル / 削除するを明確に分ける

- `screenshots/capture-designs/430x932/mobile_article_list.png`: モバイル版 記事一覧画面
  - 画面幅に合わせたカード表示
  - 左サイドバーを隠し、上部ハンバーガーと下部ナビで主要導線に到達できる

- `screenshots/capture-designs/430x932/mobile_drawer.png`: モバイル版 ナビゲーションドロワー
  - すべて / 未読 / 既読 / お気に入り / カレンダー / タグ管理 / アカウント / ログアウト / 言語切替を表示

- `screenshots/capture-designs/430x932/mobile_add_article_modal.png`: モバイル版 記事追加モーダル
  - 全画面モーダルとして表示し、ヘッダー内に閉じる・保存を配置

- `screenshots/capture-designs/430x932/mobile_detail_view.png`: モバイル版 記事詳細画面 / 閲覧
  - 1カラムで本文とメタ情報を縦積みにする

- `screenshots/capture-designs/430x932/mobile_detail_edit.png`: モバイル版 記事詳細画面 / 編集
  - 「記事の詳細」アコーディオンをメモ上に配置し、メモ編集とプレビューの高さ揺れを抑える

- `screenshots/capture-designs/430x932/mobile_filter_dialog.png`: モバイル版 フィルタモーダル
  - 全画面モーダルでタグ、おすすめ度、日付範囲を調整する

- `screenshots/capture-designs/430x932/mobile_calendar_day_sheet.png`: モバイル版 カレンダー日別記事一覧
  - 日付セルタップでその日の記事を bottom sheet として表示し、記事詳細へ遷移できる
