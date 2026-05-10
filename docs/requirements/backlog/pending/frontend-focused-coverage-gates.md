# Frontend focused coverage gates

## 状態

未対応

## 優先度

P2

## 目的

全体 coverage threshold を現状追認の低い値に留めるのではなく、domain、composables、security-sensitive utilities に絞った高い品質ゲートを追加し、重要ロジックの検証密度を説明できる状態にする。

## 対象

- `frontend/vite.config.ts`
- frontend test / coverage scripts
- article domain helpers、主要 composables、Markdown sanitizer、API client、JWT / session utilities
- testing / CI docs

## 対応内容

- global coverage threshold は段階的改善用として維持しつつ、重要領域向けの focused coverage check を追加する。
- `articleFilters`、calendar helpers、Markdown rendering / sanitization、API client、JWT decode / session refresh、主要 article composables を対象候補にする。
- focused gate は CI で説明しやすい script 名にし、通常の unit coverage と役割を分ける。
- 閾値は対象領域の既存 coverage を確認して、過度に壊れやすくないが現状追認に見えない水準へ設定する。

## 完了条件

- 重要 frontend ロジックに対して global threshold より高い focused coverage gate が存在する。
- CI / testing docs に global coverage と focused coverage の役割分担が明記される。
- security-sensitive utilities のテスト不足がある場合は、必要な unit test が追加される。
- `npm run test:unit:coverage` または追加 script が CI 上で再現できる。

## 根拠

現状の coverage threshold は lines 19%、functions 14%、branches 16%、statements 19% で、品質ゲートというより現在値に合わせた最低ラインに見えやすい。ポートフォリオでは全体 coverage よりも重要領域の高い保証を示す方が説得力がある。
