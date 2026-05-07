# ReadStack Design Assets

このディレクトリには、ReadStack の現行実装から取得したスクリーンショットを格納します。

## 現在の管理対象

- `desktop_article_list.png`: デスクトップ版の記事一覧
- `desktop_article_detail_light.png`: デスクトップ版の記事詳細
- `add_article_modal.png`: 記事追加モーダル
- `mobile_article_list.png`: モバイル幅での一覧表示

## 更新方法

1. `docker compose up --build -d` でアプリを起動する
2. `frontend` で `npm run capture:designs` を実行する
   - ブラウザ locale は `ja-JP` とする
   - デスクトップ版スクリーンショットは `1920x1080` で取得する
3. 差し替え前の画像は `archive/<YYYY-MM-DD>/` に移動して残す
4. UI 仕様との差分が出た場合は `docs/design.md` も同じ作業内で更新する

## archive

- `archive/` には、差し替え前の画像を日付単位で保存する
- 現行の説明に使うのはルート直下の4枚だけとする
