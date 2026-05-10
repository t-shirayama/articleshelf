# Frontend focus management と reduced motion 改善

## 状態

未対応

## 優先度

P2

## 目的

Dialog 開閉、詳細から戻る操作、カレンダー日付 dialog、アニメーション表示で、キーボード利用者や motion sensitivity のある利用者に配慮した UI にする。

## 対象

- ArticleFormModal
- DeleteConfirmDialog
- Calendar day dialog / bottom sheet
- Article detail back navigation
- Account settings dialog
- global CSS transitions / animations
- accessibility tests

## 対応内容

- dialog を閉じた後に、開いた操作元へ focus を戻す
- 詳細から一覧 / カレンダーへ戻ったときの focus 復帰を検討する
- Calendar day dialog を閉じた後に元の日付セルへ focus を戻す
- `prefers-reduced-motion: reduce` の CSS を追加する
- Playwright または component tests で主要 focus flow を確認する

## 完了条件

- 主要 dialog の focus 復帰先が明確になっている
- keyboard only で主要操作を完了できる
- reduced motion 設定時に不要な animation / transition が抑制される
- accessibility / responsive docs と tests が更新されている

## 根拠

Frontend レビューで、dialog 開閉や詳細から戻ったときの focus management、Calendar day dialog の focus 復帰、`prefers-reduced-motion` 対応を明示すると UX / accessibility が上がると指摘されたため。
