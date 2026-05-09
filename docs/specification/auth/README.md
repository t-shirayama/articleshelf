# ユーザー登録・ログイン・JWT認証仕様

最終更新: 2026-05-08

## 1. 目的

ArticleShelf はユーザーごとに記事、タグ、メモ、既読履歴を分離する。
認証はメール運用を前提にせず、`username + password` をログイン ID とする。

## 2. 方針サマリー

- 認証 ID は `username` とする
- メール確認、メール送信型パスワードリセット、SMTP 運用、メール文面管理は対象外
- API 認証は短命な JWT access token を使う
- refresh token は HttpOnly cookie として扱い、DB には HMAC hash のみ保存する
- access token はフロントエンドのメモリ上に保持し、localStorage には保存しない
- 記事、タグ、記事タグ紐づけは必ず `user_id` に紐づける
- 検索、詳細、更新、削除はすべて認証ユーザーの `user_id` でスコープする
- ユーザー間では同じ URL や同じタグ名を登録できる
- 退会は `users.status = DELETED` の論理削除とし、記事・タグは保持するが本人を含めて参照不可にする
- パスワード変更、退会、管理者リセットでは refresh token を全失効し、`token_valid_after` を更新する
- protected API は JWT の署名・期限だけでなく、ユーザーが `ACTIVE` であり token が `token_valid_after` より古くないことも確認する
- 通常起動では初期ユーザーを自動作成しない。初期 ADMIN が必要な検証環境だけ `ARTICLESHELF_INITIAL_USER_ENABLED=true` を明示する
- 登録 / ログインの公開 API は backend の in-memory レート制限で保護する。client IP は Spring が確定した remote address を使い、単一インスタンス向けの簡易制限とする。複数インスタンスでは共有ストア、proxy、WAF 側制限を別途使う

## 3. Username

username はログイン ID として使う。

- 登録・ログイン時は `trim` と小文字化を行う
- 3〜32文字
- 使用可能文字は `a-z`, `0-9`, `.`, `_`, `-`
- DB unique 制約は正規化後の `users.username` に適用する
- 表示名が未入力の場合は username を表示名として使う
- 既存 DB からの移行では `users.email` の local-part を元に username を生成し、重複時は suffix を付ける
- `users.email` は移行互換用の nullable legacy column とし、新規 API では返さない

## 4. Protected API

記事・タグ API はすべて認証必須にする。

- `Authorization` header の access token を検証する
- `sub` の user id を application service へ渡す
- 一覧は `user_id = currentUser.id` の記事のみ返す
- 詳細、更新、削除は `id` と `user_id` の両方で検索する
- 他ユーザーの記事 ID / タグ ID は存在しないものとして `404 Not Found` を返す
- application 層の検証に加え、`article_tags` の複合 FK で article / tag の user mismatch を拒否する

## 5. 初期データ

- 通常の `docker compose up --build` ではユーザー、記事、タグを自動投入しない
- `ARTICLESHELF_INITIAL_USER_ENABLED=true` の場合だけ、起動時に `ARTICLESHELF_INITIAL_USERNAME` / `ARTICLESHELF_INITIAL_USER_PASSWORD` の ADMIN ユーザーを作成または再利用する
- 起動時初期ユーザー作成は管理者リセット検証や legacy 開発 DB の所有者補完用であり、通常利用のデモデータ投入には使わない
- E2E はテストごとに一意 username / URL を作成し、既存データには依存しない

## 6. 詳細文書

- [Token / Cookie / CSRF](tokens.md)
- [アカウント API](account-api.md)
- [フロントエンド認証状態](frontend.md)
- [認証テスト観点](tests.md)
- [データモデル](../data/README.md)
