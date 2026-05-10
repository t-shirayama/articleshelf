# Article optimistic locking

## 状態

未対応

## 優先度

P1

## 目的

複数タブや複数端末から同じ記事を編集した場合の lost update を検出し、競合を API と UI で説明できる更新契約にする。

## 対象

- `ArticleEntity`
- `Article` domain model / `ArticleResponse`
- `UpdateArticleUseCase`
- `PUT /api/articles/{id}` request / response
- frontend article detail edit flow
- API / feature / data / frontend design / testing docs

## 対応内容

- `ArticleEntity` に JPA optimistic locking 用の version を追加し、domain / response / update request へ必要な version contract を通す。
- 更新時に client が見ていた version を送信し、保存時に現行 version と異なる場合は競合として扱う。
- 競合時は machine-readable error code を持つ 409 系 response にし、frontend で再読み込みや編集内容の扱いを案内できるようにする。
- 既存の OGP metadata fetch、URL 重複確認、tag 解決、transaction 境界と optimistic locking の順序を整理する。
- migration、API docs、frontend UI 文言、E2E / integration test を同じ実装タスク内で同期する。

## 完了条件

- 同じ記事を古い version で更新した場合に lost update せず、競合エラーとして返る。
- `ArticleResponse` と update request の version 契約が API docs に反映される。
- frontend の記事詳細編集で競合時の表示と回復導線がある。
- PostgreSQL integration test または API integration test で競合更新を確認する。
- 既存の通常更新、URL 変更、タグ変更、ステータス変更が回帰しない。

## 根拠

現在の記事更新は、対象記事を読み込み、必要に応じて OGP metadata を取得し、保存する流れだが、更新元の version 確認が見当たらない。複数タブや複数端末で同じ記事を編集した場合、後勝ちで変更が失われる可能性があり、面接でも concurrency policy として聞かれやすい。
