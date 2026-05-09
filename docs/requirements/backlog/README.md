# ArticleShelf Backlog

最終更新: 2026-05-10

このディレクトリは、ArticleShelf の今後やるべきこと、作業予定、構想段階の案、TODO、技術的負債を集約する唯一の Backlog です。
仕様として確定している将来拡張方針は該当する仕様書に残し、作業予定・構想・TODO は Backlog に集約します。

## 状態別索引

- [未対応](pending/README.md): まだ着手していないタスク
- [対応中](in-progress/README.md): 現在対応中のタスク
- [完了アーカイブ](archive/README.md): 完了済みタスクの月別要約

## Backlog Rules

- タスクは1ファイル1タスクで管理する
- ファイル名は英小文字 kebab-case とし、内容が分かる短い名前にする
- タスク本文は次の見出しを標準にする
  - `# タスク名`
  - `## 状態`
  - `## 優先度`
  - `## 目的`
  - `## 対象`
  - `## 対応内容`
  - `## 完了条件`
  - `## 根拠`
- 状態変更はファイル移動で表す
- 未対応タスクは `pending/` に置く
- 対応中タスクは `in-progress/` に置く
- 完了タスクは `archive/YYYY-MM.md` へ要約を追記し、タスクファイルは削除する
- 各フォルダの `README.md` は必ず索引として更新する
- ドキュメント内に TODO、TBD、要確認、残作業 が出た場合は、原則 Backlog の具体的なタスクへ変換する

## 運用メモ

- Backlog には現在仕様を再定義しない
- 仕様化された内容は `docs/specs/`、要件化された内容は `docs/requirements/functional/` または `docs/requirements/non-functional/`、設計判断は `docs/architecture/` または `docs/designs/` に反映する
- 一時対応や既知の制約を入れた場合は、現在仕様として必要な説明を該当 docs に反映し、残る作業だけを Backlog タスクにする
