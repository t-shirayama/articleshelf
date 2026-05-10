# Frontend 検索・フィルタ仕様の契約整理

## 状態

未対応

## 優先度

P2

## 目的

Frontend と Backend の検索対象、タグフィルタ条件、locale-aware sort、日付比較の仕様差を整理し、ユーザーにとって一貫した検索 / フィルタ体験にする。

## 対象

- `frontend/src/features/articles/domain/articleFilters.ts`
- `frontend/src/features/articles/domain/articleForms.ts`
- `frontend/src/features/articles/composables/useTagManagementState.ts`
- `docs/specs/features/tags-and-search.md`
- `docs/specs/ui/README.md`
- backend search contract

## 対応内容

- Frontend search 対象に `summary` を含めるか検討し、Backend search と合わせる
- tag filter が OR 条件か AND 条件かを仕様化し、UI 表示に反映する
- `localeCompare` の locale 固定を見直し、現在 locale に応じた比較へ寄せる
- date parsing / date key 比較を `shared/utils/date` などへ共通化する
- タグ順を保持するか、正規化時に sort / Set 比較するかを仕様化する

## 完了条件

- Frontend / Backend の検索対象差分が解消または仕様として明記されている
- タグフィルタ条件が UI と specs に明示されている
- locale-aware sort / search helper の責務が整理されている
- 日付比較 helper が重複しすぎない形に整理されている
- 関連 unit tests と specs が更新されている

## 根拠

Frontend レビューで、Backend search は title / url / summary / notes を対象にする一方、Frontend search は title / url / notes / tags が中心で、検索結果のズレにつながる可能性があると指摘されたため。
