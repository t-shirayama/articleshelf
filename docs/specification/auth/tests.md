# 認証テスト観点

- username 正規化、形式不正、重複を検証する
- password が username と同一の場合に拒否する
- username 登録、login、refresh、logout の一連フローを検証する
- パスワード変更、全端末ログアウト、退会、管理者リセットで refresh token が全失効することを検証する
- 全端末ログアウト後は、他端末を含む既発行 access token が protected API で `401` になることを検証する
- `token_valid_after` より古い JWT と `DELETED` user の JWT を拒否する
- 退会後に login、refresh、protected API が使えないことを検証する
- `ADMIN` のみ管理者パスワードリセットを実行できることを検証する
- フロントエンドでは auth API adapter、auth store、アカウント操作後の state clear を検証する
- E2E では username 登録/ログイン、パスワード変更、全端末ログアウト、退会、管理者リセットを確認する
