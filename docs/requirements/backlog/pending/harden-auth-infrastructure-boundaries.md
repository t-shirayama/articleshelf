# 認証インフラ境界と運用シグナル強化

## 状態

未対応

## 優先度

中

## 目的

reverse proxy 配下の client IP 解決、rate limit の差し替え可能性、invalid token の観測性を整え、認証インフラの境界を明確にする。

## 対象

- AuthController / UserController の client IP 取得
- login / auth rate limit
- `JwtAuthenticationFilter`
- auth metrics / structured log
- security specs / quality specs

## 対応内容

- trusted proxy 前提を明示した `ClientIpResolver` を追加する
- login rate limit を application port または dedicated interface に切り出す
- 将来的に Redis / Bucket4j へ差し替えられる境界を用意する
- invalid / expired token の metrics または structured log を追加する
- token 値や secret をログに出さない方針を明確にする

## 完了条件

- client IP 解決が専用コンポーネントに分離されている
- rate limit の呼び出し境界が Controller 内の直接実装から切り出されている
- invalid token / expired token / unauthorized などの運用シグナルが token 値を漏らさず記録できる
- security / quality / testing docs が更新されている

## 根拠

Backend レビューで、`AuthController.clientIp()` が `request.getRemoteAddr()` を返していること、rate limit が Controller 内に寄っていること、invalid token 時の観測性を高める余地があることを指摘されたため。
