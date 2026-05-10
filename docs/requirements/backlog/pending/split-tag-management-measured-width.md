# TagManagement DOM 計測責務分離

## 状態

未対応

## 優先度

P3

## 目的

`useTagManagementState` に混在しているタグ管理状態と DOM 計測を分け、状態管理 composable の責務を明確にする。

## 対象

- `frontend/src/features/articles/composables/useTagManagementState.ts`
- tag search / sort state
- add / rename / merge / delete dialog state
- sort select width measurement
- tag management component tests

## 対応内容

- sort select width の DOM 計測を `useMeasuredSelectWidth` などへ切り出す
- `useTagManagementState` はタグ管理の UI state と action state に集中させる
- locale 文言変更時の width 再計算を維持する
- 既存のタグ管理 UI とソート表示を維持する

## 完了条件

- DOM measurement が dedicated composable に分離されている
- tag management state と DOM 依存処理の境界が明確になっている
- 既存のタグ検索、並び替え、追加、rename、merge、delete の挙動が維持されている
- 必要な tests と design docs が更新されている

## 根拠

Frontend レビューで、`useTagManagementState` が dialog state、search / sort、sort width DOM measurement を同時に持っており、DOM 依存とタグ管理状態を分けるとよりきれいだと指摘されたため。
