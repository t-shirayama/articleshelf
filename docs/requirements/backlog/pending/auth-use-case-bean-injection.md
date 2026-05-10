# Auth use case bean injection

## 状態

未対応

## 優先度

P2

## 目的

`AuthService` 内で認証 use case helper を `new` する構成をやめ、Spring bean として constructor injection し、依存関係、テスト、責務境界を見やすくする。

## 対象

- `AuthService`
- `RefreshTokenRotationService`
- `InitialUserProvisioner`
- auth application tests
- backend architecture / auth specs / testing docs

## 対応内容

- `RefreshTokenRotationService` と `InitialUserProvisioner` を Spring 管理 bean として注入できる構成にする。
- `AuthService` の constructor から helper 構築用の依存を減らし、AuthService 自身の責務を orchestration に寄せる。
- 既存の refresh token rotation、initial user provisioning、register / login / refresh の挙動を維持する。
- unit test で helper を差し替えやすくし、DI 境界を architecture docs に反映する。

## 完了条件

- `AuthService` が `new RefreshTokenRotationService(...)` と `new InitialUserProvisioner(...)` を持たない。
- 対象 helper が Spring bean として constructor injection される。
- auth tests が既存挙動を保ったまま通る。
- backend architecture docs に auth application service と helper use case の依存関係が反映される。

## 根拠

現状は Spring DI を使っているにもかかわらず、`AuthService` が `RefreshTokenRotationService` と `InitialUserProvisioner` を constructor 内で生成している。動作上は成立するが、依存関係と責務境界の説明では bean injection に揃えた方が自然で、テストもしやすい。
