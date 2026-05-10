# Machine-readable API エラー導入

## 状態

未対応

## 優先度

P2

## 目的

ユーザー向けメッセージとシステム向けエラーコードを分離し、フロントエンド、運用、テストで扱いやすい API エラーレスポンスにする。

## 対象

- `backend/src/main/java/com/articleshelf/adapter/web/ApiExceptionHandler.java`
- `ErrorResponse`
- validation / domain / auth / framework error
- frontend API error handling
- API specs / error response docs

## 対応内容

- `code`、`fieldErrors`、`requestId` など machine-readable な項目を検討する
- 既存の `messages` と i18n 表示を維持しつつ、機械判定用コードを追加する
- duplicate URL、invalid URL、validation error、auth error など主要エラーにコードを割り当てる
- 必要に応じて Spring `ProblemDetail` への寄せ方を検討する
- frontend のエラー表示と E2E / API tests を追従させる

## 完了条件

- API エラーに機械判定可能な `code` または同等の項目が含まれている
- validation error は field ごとの code / message を扱える
- 既存のユーザー向けメッセージ表示が維持されている
- `docs/specs/api/README.md` または error docs が更新されている

## 根拠

Backend / Frontend レビューで、現在の `ErrorResponse` は主に `messages` 中心であり、ユーザー向けメッセージとシステム向けエラーコードを分けるとプロダクション運用に強くなると指摘されたため。
