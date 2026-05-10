# カレンダー domain helper と keyboard 操作改善

## 状態

未対応

## 優先度

P2

## 目的

CalendarView 内の日付計算を domain helper へ切り出し、日付セルの keyboard 操作を改善して、テストしやすくアクセシブルなカレンダーにする。

## 対象

- `frontend/src/features/articles/components/CalendarView.vue`
- calendar date / month calculation
- calendar cell generation
- created / read mode
- day dialog opening
- keyboard operation

## 対応内容

- `features/articles/domain/calendar.ts` を追加し、日付 key、月 key、visible cells 生成を切り出す
- `createCalendarCells` などの純粋関数として unit test できる形にする
- 日付セルを button 化する、または `tabindex` と Enter / Space handler を追加する
- day dialog を閉じた後の focus 復帰を検討する
- 既存のカレンダー UI と mobile bottom sheet 仕様を維持する

## 完了条件

- カレンダー日付計算が component から分離されている
- calendar domain helper に unit tests がある
- キーボードで日付セルを開ける
- focus management と responsive 仕様が docs / tests に反映されている

## 根拠

Frontend レビューで、`CalendarView.vue` に日付計算がまとまっており、`role="gridcell"` の日付セルが click 中心で keyboard 操作が弱いと指摘されたため。
