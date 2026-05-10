# OGP 取得の transaction 外分離

## 状態

未対応

## 優先度

P1

## 目的

外部 HTTP アクセスである OGP 取得を DB transaction 内から分離し、外部サイトの遅延や失敗が DB transaction に波及しにくい構造にする。

## 対象

- article add / update flow
- `ArticleMetadataProvider`
- `ArticleService` または分割後の Add / Update use case
- OGP fetch timeout / error handling
- article persistence transaction boundary

## 対応内容

- 現行の OGP 取得が transaction 内で行われている箇所を特定する
- 同期処理のまま、URL 重複チェック、OGP 取得、短い transaction での保存に分ける案を検討する
- 将来的な非同期 metadata 補完の余地を整理する
- 非同期化する場合は metadata status、retry、エラー表示、再取得導線を仕様化する
- OGP 取得失敗時の既存 UX / API 契約を維持または明示的に変更する

## 完了条件

- 外部 HTTP fetch が DB transaction 内で実行されない
- article add / update の transaction boundary が明確になっている
- OGP 取得失敗、timeout、SSRF guard 失敗時の挙動が specs / security docs に反映されている
- 関連する application / persistence / integration tests が更新されている

## 根拠

Backend レビューで、`ArticleService.addArticle()` / `updateArticle()` の transaction 内で `metadataProvider.fetch()` が呼ばれており、外部 HTTP アクセスの遅延や失敗が DB 処理に波及しやすいと指摘されたため。
