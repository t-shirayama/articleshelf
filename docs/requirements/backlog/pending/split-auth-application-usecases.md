# Auth アプリケーションユースケース分割

## 状態

未対応

## 優先度

P2

## 目的

`AuthService` に集中している register、login、refresh、logout、password change、account delete、admin reset、initial user 作成の責務を分割し、認証ユースケースと refresh token rotation の設計意図を明確にする。

## 対象

- `backend/src/main/java/com/articleshelf/application/auth/AuthService.java`
- refresh token rotation
- user registration / login / logout / refresh
- account controls
- admin password reset
- initial user provision

## 対応内容

- `RegisterUserUseCase`、`LoginUseCase`、`RefreshSessionUseCase`、`LogoutUseCase`、`ChangePasswordUseCase`、`DeleteAccountUseCase`、`ResetPasswordByAdminUseCase` などへ責務を分ける
- `RefreshTokenRotationService` を切り出し、token family、reuse detection、family revoke の責務を明確にする
- initial user 作成を `InitialUserProvisioner` などの独立責務へ寄せる
- 既存の security / auth API 契約を維持する

## 完了条件

- `AuthService` の肥大化が解消され、主要操作がユースケース単位に分かれている
- refresh token rotation の責務が独立している
- 既存の register、login、refresh、logout、logout all、password change、delete account、admin reset の挙動が維持されている
- 関連する auth tests、security specs、architecture docs が更新されている

## 根拠

Backend レビューで、`AuthService` が多くの認証 / アカウント操作をまとめて持っており、特に refresh token rotation はセキュリティ設計として独立させると伝わりやすいと指摘されたため。
