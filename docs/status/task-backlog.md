# ArticleShelf Task Backlog

最終更新: 2026-05-09

このドキュメントは、ArticleShelf の残タスクを優先度ごとに追うための一覧です。  
実装済み機能の棚卸しや仕様との差分は `docs/status/project-status.md` を参照し、このファイルでは「次に何を進めるか」を見やすく整理します。

## 1. 優先度高

### 1.1 本番運用で様子を見る項目

- 公開環境 smoke test / 監視方針整理

## 2. 優先度中

### 2.1 UI / UX の拡張

- お気に入り導線の強化
- モバイル操作の磨き込み
  - 実機での 375px / 390px / 600px / 820px 表示の継続確認
  - 長い日本語 / English 文言、タグ数が多いケース、Markdown 表示の追加確認

### 2.2 既存機能の補強

- OGP サムネイルの手動再取得
- IndexedDB 外への画像ファイル保存
- 検索 / フィルタ / 並び替えの性能検証

### 2.3 アカウント運用の追加

- 退会後データの物理削除ポリシー整備
- データエクスポート

## 3. 優先度低

### 3.1 テスト基盤の拡張候補

- OGP stub server の採用検討
- Testcontainers PostgreSQL の採用検討
- mobile project E2E の対象ケース拡充

### 3.2 将来拡張

- Chrome 拡張機能からのクイック登録
  - 閲覧中ページを未読として即登録
  - 閲覧中ページを既読として即登録
- OCR による記事情報抽出
- AI 要約 / ハイライト
- ユーザーデータ削除ポリシー整備

## 4. 運用メモ

- 実装済み機能、既知差分、技術的注意点は `docs/status/project-status.md` を正とする
- 構想段階でまだ優先度を上げない案は `docs/requirements/future-considerations.md` に置き、具体化したらこの backlog へ移す
- UI の具体タスクは `docs/designs/mobile-responsive.md`、公開構成の具体タスクは `docs/deployment/README.md` も併せて参照する
