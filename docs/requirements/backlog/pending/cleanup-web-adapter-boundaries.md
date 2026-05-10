# Web adapter 境界の責務整理

## 状態

未対応

## 優先度

中

## 目的

Controller 内に混在している request DTO、request mapper、client context 取得、例外処理補助を整理し、Web adapter を HTTP 境界として読みやすくする。

## 対象

- `backend/src/main/java/com/articleshelf/adapter/web/ArticleController.java`
- `backend/src/main/java/com/articleshelf/adapter/web/AuthController.java`
- `backend/src/main/java/com/articleshelf/adapter/web/UserController.java`
- `backend/src/main/java/com/articleshelf/adapter/web/AdminController.java`
- `backend/src/main/java/com/articleshelf/adapter/web/ApiExceptionHandler.java`
- request DTO / request mapper / common web helpers

## 対応内容

- Controller inner record になっている request DTO を必要に応じて独立ファイルへ移す
- request DTO から command / use case input への変換を request mapper として整理する
- User-Agent / IP など client context 取得の責務を共通 helper へ寄せる
- `ApiExceptionHandler` の i18n、型変換、domain / auth / framework 例外処理を整理し、必要なら補助クラスへ分割する
- Web adapter の package 構成を `common`、`article`、`auth`、`user` などの責務で見直す

## 完了条件

- Controller が HTTP request / response の受け渡しと application 呼び出しに集中している
- request DTO と mapper の配置が責務ごとに整理されている
- common web helper が adapter 層内で再利用できる
- 既存 API 契約と error response が維持または仕様化された変更として反映されている
- API / architecture docs と関連 tests が更新されている

## 根拠

Backend レビューで、Request DTO が Controller の inner record になっていること、Cookie / CSRF / rate limit / client IP 取得が Controller に混在していること、`ApiExceptionHandler` が多くの責務を背負っていることを指摘されたため。
