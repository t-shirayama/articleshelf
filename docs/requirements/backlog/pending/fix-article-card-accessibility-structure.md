# ArticleCard accessibility 構造整理

## 状態

未対応

## 優先度

P1

## 目的

ArticleCard の親要素と内部操作ボタンの nested interactive 構造を整理し、支援技術とキーボード操作で扱いやすいカードにする。

## 対象

- `frontend/src/features/articles/components/ArticleCard.vue`
- article card open detail interaction
- favorite / status / delete buttons
- keyboard operation
- article card component tests / E2E

## 対応内容

- カード全体を `role="button"` にする構造を見直す
- 詳細遷移は主領域のみ button / link にする、またはタイトルを button にする
- favorite / status / delete は主領域の open detail とイベント的にも意味的にも分離する
- Enter / Space 操作と focus order を確認する
- 既存の見た目を維持しつつ HTML semantics を改善する

## 完了条件

- nested interactive controls が解消されている
- キーボードで詳細遷移、favorite、status、delete を意図どおり操作できる
- screen reader / axe で重大な card 構造問題が出ない
- ArticleCard tests と design / accessibility docs が更新されている

## 根拠

Frontend レビューで、`VCard` に `role="button"` と `tabindex="0"` を付けた親の中に複数の `VBtn` があり、動作はしてもアクセシビリティ上は複雑だと指摘されたため。
