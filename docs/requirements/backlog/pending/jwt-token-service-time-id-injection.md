# JWT token service time id injection

## 状態

未対応

## 優先度

P2

## 目的

`JwtTokenService` の access token 発行時刻と JWT ID 生成を `Clock` / `IdGenerator` injection に揃え、認証処理全体の deterministic testability を高める。

## 対象

- `JwtTokenService`
- `Clock` / `IdGenerator` bean
- `JwtTokenServiceTest`
- auth / security / backend architecture / testing docs

## 対応内容

- `Instant.now()` を `Clock` injection に置き換える。
- `UUID.randomUUID()` を既存の `IdGenerator` injection に置き換える。
- JWT `iat` / `exp` / `jti` を固定値で検証できる test に更新する。
- refresh token 周辺ですでに使っている time / id provider 方針と auth infrastructure を揃える。

## 完了条件

- `JwtTokenService` が直接 `Instant.now()` と `UUID.randomUUID()` を呼ばない。
- JWT 発行 test が固定 clock / fixed id generator で `issuedAt`、`expiresAt`、`jti` を検証できる。
- auth / security docs に access token 発行も injected time / id provider に従うことが反映される。
- 既存の JWT parse、invalid token、expired token のテストが回帰しない。

## 根拠

`JwtTokenService` は Spring Security JOSE を使っており実装基盤は良いが、時刻と ID の生成だけ直接呼び出しになっている。他の認証処理では `Clock` や `IdGenerator` を注入しているため、ここも一貫させるとテスト性と設計説明が改善する。
