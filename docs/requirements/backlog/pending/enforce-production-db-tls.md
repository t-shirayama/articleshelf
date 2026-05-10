# Production DB TLS 起動ガード追加

## 状態

未対応

## 優先度

P1

## 目的

公開構成で DB 接続 TLS を必須にするという security / deployment docs の方針を、production profile の起動時検証でも強制する。

## 対象

- `backend/src/main/java/com/articleshelf/config/ProductionEnvironmentValidator.java`
- `backend/src/main/resources/application-prod.yml`
- `SPRING_DATASOURCE_URL`
- PostgreSQL JDBC `sslmode`
- deployment / security docs

## 対応内容

- production profile の datasource URL 取得方法を確認する
- `sslmode=require`、`verify-ca`、`verify-full` のいずれかが指定されていない場合は起動拒否する
- Supabase direct / pooler URL の指定例と整合する検証にする
- dev / test profile では既存のローカル起動を壊さない
- validator tests を追加または更新する

## 完了条件

- production profile で PostgreSQL JDBC URL に `sslmode=require` 以上がない場合は起動しない
- `sslmode=require` / `verify-ca` / `verify-full` は許可される
- security / deployment docs と実装が一致している
- 関連 tests が追加または更新されている

## 根拠

Security レビューで、docs では公開構成の DB 接続に TLS と `sslmode=require` 以上を要求している一方、`ProductionEnvironmentValidator` は datasource URL の `sslmode` を検証しておらず、ドキュメントと実装にズレがあると指摘されたため。
