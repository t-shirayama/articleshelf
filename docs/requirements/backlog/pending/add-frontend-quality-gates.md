# Frontend quality gates 強化

## 状態

未対応

## 優先度

P2

## 目的

Frontend の lint、coverage threshold、accessibility scan、bundle size check、component test を整え、CI 上の品質ゲートを強化する。

## 対象

- frontend package scripts
- Vitest coverage
- ESLint / Prettier / stylelint
- Playwright accessibility checks
- bundle size visualization / check
- GitHub Actions frontend jobs
- testing docs

## 対応内容

- ESLint / Prettier / stylelint の導入要否と範囲を検討する
- `npm run lint` を追加し、CI に組み込む
- frontend unit coverage threshold を設定する
- `@axe-core/playwright` などで主要画面の accessibility scan を追加する
- bundle size check または visualizer 出力を CI / docs に追加する
- ArticleFormModal、ArticleCard、CalendarView などの component tests を追加する

## 完了条件

- frontend lint が CI で実行されている
- frontend coverage threshold が定義されている
- 主要画面の accessibility scan が CI または E2E に組み込まれている
- bundle size の確認方法が追加されている
- testing docs と README / architecture docs が必要に応じて更新されている

## 根拠

Frontend レビューで、typecheck / unit / integration / E2E は強い一方、lint、accessibility、自動 coverage threshold、bundle size check がないため、チーム開発や大規模開発の品質ゲートとして補強余地があると指摘されたため。
