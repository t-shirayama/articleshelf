# Backend quality gates の見せ方整理

## 状態

未対応

## 優先度

低

## 目的

Clean Architecture dependency test、SpotBugs、coverage threshold、PostgreSQL integration tests、E2E などの backend 品質ゲートを README や architecture docs で伝わりやすく整理する。

## 対象

- `README.md`
- `docs/architecture/backend/README.md`
- `docs/architecture/ci-cd/README.md`
- `docs/testing/README.md`
- CI workflow

## 対応内容

- backend quality gates として説明すべき確認項目を整理する
- README または docs に、初見のレビュアへ伝わりやすい短い説明を追加する
- Clean Architecture dependency test、SpotBugs、coverage、PostgreSQL IT、E2E の関係を明確にする
- 既存 CI 実行内容と docs の表現にズレがないか確認する

## 完了条件

- backend の品質ゲートが README または docs から把握できる
- CI workflow と docs の説明が一致している
- 追加した説明が testing / architecture の責務を重複しすぎていない

## 根拠

Backend レビューで、既存の architecture test、SpotBugs、coverage threshold、PostgreSQL integration tests、E2E は強みなので、README や docs で目立たせるとポートフォリオとして伝わりやすいと指摘されたため。
