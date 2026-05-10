# Frontend app providers 分離

## 状態

未対応

## 優先度

P3

## 目的

`main.ts` にある Pinia、i18n、Vuetify provider 設定を分離し、app bootstrap を薄くして初見の見通しを良くする。

## 対象

- `frontend/src/main.ts`
- Pinia setup
- i18n setup
- Vuetify theme / defaults setup
- global CSS import
- frontend architecture docs

## 対応内容

- `src/app/providers/pinia.ts`、`vuetify.ts`、`i18n.ts` などへの分割を検討する
- Vuetify theme / default component props を provider helper へ移す
- `main.ts` は Vue app 作成と provider 登録に集中させる
- 既存の theme、locale、component defaults、CSS 読み込みを維持する

## 完了条件

- `main.ts` が app bootstrap の入口として薄くなっている
- provider 設定が責務ごとのファイルに分かれている
- 既存 UI theme と i18n / Vuetify locale が維持されている
- frontend architecture docs が更新されている

## 根拠

Frontend レビューで、`main.ts` は責務が明確だが、Vuetify setup などを `app/providers` へ切り出すと設計説明力と見通しが上がると指摘されたため。
