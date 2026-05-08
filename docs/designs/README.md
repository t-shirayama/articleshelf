# ReadStack Design Assets

このディレクトリには、ReadStack の現行実装から取得したスクリーンショットを格納します。

## 現在の管理対象

- `auth_login.png`: ログイン画面
- `desktop_article_list.png`: デスクトップ版の記事一覧
- `desktop_article_detail_light.png`: デスクトップ版の記事詳細
- `calendar_view.png`: カレンダー画面
- `tag_management.png`: タグ管理画面
- `add_article_modal.png`: 記事追加モーダル
- `filter_dialog.png`: フィルタモーダル
- `tag_add_dialog.png`: タグ追加モーダル
- `tag_merge_dialog.png`: タグ統合モーダル
- `tag_delete_dialog.png`: タグ削除確認ダイアログ
- `delete_article_dialog.png`: 記事削除確認ダイアログ
- `mobile_article_list.png`: モバイル幅での一覧表示

## 更新方法

1. `docker compose up --build -d` でアプリを起動する
2. `frontend` で `npm run capture:designs` を実行する
   - ブラウザ locale は `ja-JP` とする
   - デスクトップ版スクリーンショットは `1920x1080` で取得する
   - キャプチャ用ユーザーと記事/タグはスクリプト内で作成し、ログイン状態や既存データに依存しない
3. 差し替え前の画像は `archive/<YYYY-MM-DD>/` に移動して残す
4. UI 仕様との差分が出た場合は `docs/design/README.md` も同じ作業内で更新する

## archive

- `archive/` には、差し替え前の画像を日付単位で保存する
- 現行の説明に使うのはルート直下の画像だけとする
