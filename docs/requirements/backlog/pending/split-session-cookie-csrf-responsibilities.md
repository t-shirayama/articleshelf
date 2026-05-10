# セッション Cookie と CSRF 責務の分離

## 状態

未対応

## 優先度

高

## 目的

AuthController / UserController に混在している session cookie 操作と CSRF 検証を分離し、Controller を HTTP 入出力の薄い入口に近づける。

## 対象

- `backend/src/main/java/com/articleshelf/adapter/web/AuthController.java`
- `backend/src/main/java/com/articleshelf/adapter/web/UserController.java`
- refresh token cookie の発行 / 削除
- CSRF token 検証
- 関連する controller / auth integration tests

## 対応内容

- `SessionCookieWriter` を追加し、refresh cookie の発行 / 削除を集約する
- `CsrfTokenValidator` を追加し、CSRF cookie / header の検証を Controller から切り出す
- AuthController / UserController の cookie 操作重複を削除する
- Controller は request handling と use case 呼び出しに集中する形へ整理する
- 既存の認証、refresh、logout、account 操作の挙動を維持する

## 完了条件

- session cookie 操作が専用コンポーネントへ集約されている
- CSRF 検証が専用コンポーネントへ集約されている
- AuthController / UserController から cookie / CSRF の重複実装が削除されている
- 既存の auth 関連テストが新しい責務分離に追従している
- 必要な architecture / security / testing docs が更新されている

## 根拠

Backend レビューで、Controller が session cookie 発行、CSRF 検証、rate limit、User-Agent / IP 取得、auth service 呼び出しをまとめて持っており、HTTP 境界の責務分離を強める余地があると指摘されたため。
