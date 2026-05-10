# Frontend architecture highlights 整理

## 状態

未対応

## 優先度

P3

## 目的

Frontend の feature-oriented architecture、auth-aware API client、safe Markdown rendering、responsive UX、client-side domain helpers を docs で説明しやすくする。

## 対象

- `README.md`
- `docs/architecture/frontend/README.md`
- `docs/testing/README.md`
- `docs/specs/security/README.md`
- frontend quality / E2E docs

## 対応内容

- Frontend design highlights を architecture docs に追加する
- feature / shared / domain / api / store の責務を短く整理する
- access token 付与、refresh retry、CSRF header、API error mapping を API client の見どころとして説明する
- safe Markdown rendering と DOMPurify の責務を security docs へリンクする
- desktop / tablet / mobile の responsive UX と Playwright 検証を説明する

## 完了条件

- Frontend の主要な設計判断が architecture docs から短く把握できる
- README から frontend の技術的な見どころへ辿れる
- security / testing docs と説明が重複しすぎていない
- 現行実装と docs の説明が一致している

## 根拠

Frontend レビューで、既存 frontend には説明しやすい強みが多いため、architecture docs に design highlights としてまとめるとポートフォリオで伝わりやすいと指摘されたため。
