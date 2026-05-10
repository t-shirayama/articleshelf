# Flyway baseline migration の意図整理

## 状態

未対応

## 優先度

低

## 目的

`V1__baseline_schema.sql` に冪等な既存 DB 救済処理が含まれる理由を docs に明記し、初見のレビュアが migration 履歴を理解しやすくする。

## 対象

- `backend/src/main/resources/db/migration/V1__baseline_schema.sql`
- `backend/src/main/resources/db/migration/V2__username_auth_and_account_controls.sql`
- `docs/architecture/data/README.md`
- `docs/specs/data/README.md`

## 対応内容

- V1 が既存開発 DB からの baseline 統合のため冪等になっているか確認する
- 現行 migration を残す場合は、意図と制約を architecture / data docs に説明する
- 新規環境向けに migration を読みやすく分割する案の安全性を検討する
- 本番 DB がある場合は既存 migration の rewrite を避ける方針を明記する

## 完了条件

- V1 baseline migration の意図が docs に明記されている
- migration を整理する場合は既存 DB への影響が評価されている
- Flyway / JPA validate / PostgreSQL 確認方針が testing docs または完了要約に残っている

## 根拠

Backend レビューで、`V1__baseline_schema.sql` に `CREATE TABLE IF NOT EXISTS`、`ALTER TABLE ADD COLUMN IF NOT EXISTS`、既存制約削除、既存データ補正、FK / unique / index 追加が混在しており、初見では移行履歴が複雑に見えると指摘されたため。
