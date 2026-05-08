# ReadStack Task Backlog

最終更新: 2026-05-07

このドキュメントは、ReadStack の残タスクを優先度ごとに追うための一覧です。  
実装済み機能の棚卸しや仕様との差分は `docs/status/project-status.md` を参照し、このファイルでは「次に何を進めるか」を見やすく整理します。

## 1. 優先度高

### 1.1 本番運用前に必要な基盤整備

- CI/CD の deploy workflow 整備

## 2. 優先度中

### 2.1 UI / UX の拡張

- お気に入り導線の強化
- モバイル操作の磨き込み
  - ハンバーガードロワー
  - ボトムナビ
  - 全画面追加モーダル
  - スマホ向けカレンダー

### 2.2 既存機能の補強

- OGP サムネイルの手動再取得
- IndexedDB 外への画像ファイル保存
- 検索 / フィルタ / 並び替えの性能検証

### 2.3 認証機能の追加

- メール確認
- パスワードリセット
- 全端末ログアウト

## 3. 優先度低

### 3.1 テスト基盤の拡張候補

- OGP stub server の採用検討
- Testcontainers PostgreSQL の採用検討
- スマホ UI 実装後の mobile project E2E 追加

### 3.2 将来拡張

- Chrome 拡張機能からのクイック登録
  - 閲覧中ページを未読として即登録
  - 閲覧中ページを既読として即登録
- 画像 / スクリーンショット添付
- OCR による記事情報抽出
- AI 要約 / ハイライト
- アカウント管理
- 退会処理
- ユーザーデータ削除ポリシー整備
- クラウド同期

## 4. 運用メモ

- 実装済み機能、既知差分、技術的注意点は `docs/status/project-status.md` を正とする
- 構想段階でまだ優先度を上げない案は `docs/requirements/future-considerations.md` に置き、具体化したらこの backlog へ移す
- UI の具体タスクは `docs/design/mobile-responsive.md`、公開構成の具体タスクは `docs/deployment/free-deployment.md` も併せて参照する
