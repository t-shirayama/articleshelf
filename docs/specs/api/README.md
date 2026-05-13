# API仕様

ArticleShelf の API 仕様の入口です。

## 認証方針

現行 API はユーザー登録・ログイン・JWT 認証を必須とします。
記事 API / タグ API はすべて `Authorization: Bearer <accessToken>` が必要で、JWT の `sub` から確定したユーザー ID で記事・タグ・重複 URL 判定をスコープします。
Chrome 拡張機能向け API は通常 Web JWT とは別の拡張機能専用 opaque token を使い、記事の lookup / create / status update だけを許可します。
ログイン状態の復元には HttpOnly refresh cookie を使い、フロントエンドは access token をメモリ上に保持します。

認証方式と認証 API は [認証仕様](../auth/README.md) と [アカウント API](../auth/account-api.md) を参照してください。

## Web adapter 境界

Controller は HTTP request / response の受け渡しと application service 呼び出しに集中する。
Article request から command への変換は `ArticleRequestMapper`、session cookie は `SessionCookieWriter`、CSRF 照合は `CsrfTokenValidator`、User-Agent / client IP 取得は `ClientRequestContext` が担当する。

## 詳細文書

- [記事 API](articles.md): 記事一覧、詳細、追加、保存前プレビュー、更新、削除
- [タグ API](tags.md)
- [エラーレスポンス](errors.md)
- [認証仕様](../auth/README.md): JWT/ユーザー認証の前提
- [アカウント API](../auth/account-api.md): サインイン後状態確認やユーザー情報取得
