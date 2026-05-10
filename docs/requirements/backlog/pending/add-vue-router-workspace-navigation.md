# Vue Router による画面遷移と URL 状態同期

## 状態

未対応

## 優先度

P2

## 目的

画面状態と検索 / フィルタ / 並び替え状態を URL に同期し、deep link、ブラウザ戻る / 進む、URL共有、状態復元、E2E の開始地点を扱いやすくする。

## 対象

- frontend routing
- `/login`、`/register`
- `/articles`、`/articles/:id`
- `/calendar`
- `/tags`
- `/settings`
- search / filter / sort query parameters
- mobile / desktop navigation

## 対応内容

- Vue Router を導入し、主要画面の route を定義する
- `viewMode` ベースの画面切り替えを router-backed navigation へ段階的に移行する
- 検索語、status、tag、rating、date range、sort などを query parameter と同期する
- browser back / forward と未保存差分警告の整合を確認する
- route-level code splitting の導入要否を検討する
- E2E の開始 URL を route ベースに更新する

## 完了条件

- 主要画面へ URL から直接遷移できる
- 検索 / フィルタ / 並び替え状態が query parameter と同期している
- browser back / forward で期待どおり画面状態が復元される
- 未保存差分警告が router navigation でも働く
- UI / architecture / testing docs と E2E が更新されている

## 根拠

Frontend レビューで、Vue Router がないため画面状態が component 内の `viewMode` に閉じており、ポートフォリオとして deep link、履歴、URL共有、状態復元を説明しにくいと指摘されたため。
