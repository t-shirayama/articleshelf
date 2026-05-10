# Refresh token rotation atomic 化

## 状態

未対応

## 優先度

P1

## 目的

同じ refresh token に対する並行 refresh で複数の replacement token が発行されないよう、refresh token rotation を DB レベルで atomic にする。

## 対象

- `backend/src/main/java/com/articleshelf/application/auth/AuthService.java`
- `backend/src/main/java/com/articleshelf/infrastructure/persistence/JpaRefreshTokenRepository.java`
- refresh token entity / repository
- auth integration tests
- `docs/specs/auth/tokens.md`

## 対応内容

- 現行 refresh flow の token hash lookup、revokedAt 確認、replacement 作成、旧 token revoke の transaction 境界を確認する
- pessimistic lock、条件付き update、部分 unique index などの atomic 化方式を比較する
- 同じ family に未失効 token が複数残らない制約を検討する
- 並行 refresh の integration test を追加する
- token reuse detection と family revoke の既存挙動を維持する

## 完了条件

- 同じ refresh token の並行 refresh で replacement token が複数有効化されない
- refresh token rotation が DB transaction / lock / conditional update で atomic になっている
- 並行 refresh の回帰テストが追加されている
- auth specs / security specs / testing docs が更新されている

## 根拠

Security レビューで、同じ refresh token でほぼ同時に `/refresh` が来た場合、複数リクエストが `revokedAt == null` を見て replacement token を発行できる可能性があると指摘されたため。
