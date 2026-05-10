# TimeProvider / IdGenerator port 導入

## 状態

未対応

## 優先度

低

## 目的

`Instant.now()`、`UUID.randomUUID()`、`new SecureRandom()` などの直接利用を必要に応じて port 化し、認証や token rotation のテスト容易性を上げる。

## 対象

- `AuthService` または分割後の auth use case
- refresh token rotation
- token expiration / invalidation
- application / config layer
- auth unit tests

## 対応内容

- `TimeProvider` または `Clock` Bean の導入範囲を検討する
- `IdGenerator` の導入範囲を検討する
- `SecureRandom` の注入 / wrapping が必要な箇所を確認する
- token 有効期限、rotation、reuse detection のテストを読みやすくする
- 過剰な抽象化にならない範囲で application 層の直接依存を減らす

## 完了条件

- 時刻や ID 生成に依存する auth logic が deterministic にテストできる
- application 層での直接的な `Instant.now()` / `UUID.randomUUID()` 利用が必要最小限になっている
- config / architecture docs に必要な範囲で方針が反映されている

## 根拠

Backend レビューで、`Clock` や `IdGenerator` を Bean / port として注入すると、token rotation や有効期限のテストが読みやすくなり、設計説明力も上がると指摘されたため。
