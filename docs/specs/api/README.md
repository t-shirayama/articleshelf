# API仕様

ArticleShelf の API 仕様の入口です。

## 認証方針

現行 API はユーザー登録・ログイン・JWT 認証を必須とします。
記事 API / タグ API はすべて `Authorization: Bearer <accessToken>` が必要で、JWT の `sub` から確定したユーザー ID で記事・タグ・重複 URL 判定をスコープします。
ログイン状態の復元には HttpOnly refresh cookie を使い、フロントエンドは access token をメモリ上に保持します。

認証方式と認証 API は [認証仕様](../auth/README.md) と [アカウント API](../auth/account-api.md) を参照してください。

## 詳細文書

- [記事 API](articles.md)
- [タグ API](tags.md)
- [エラーレスポンス](errors.md)
