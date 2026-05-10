# エラーレスポンス

- フロントエンドは API リクエストへ現在の表示言語を `Accept-Language` として付与する
- API エラー文言は `Accept-Language` に応じて日本語 / 英語で返す
- `Accept-Language` が `ja` 以外、未対応、または未指定の場合は英語で返す
- エラー時のレスポンス形式は `timestamp`、`code`、`messages`、`fieldErrors` を持つ JSON
- エラー時の JSON 形状は言語に関係なく維持し、`messages` の本文だけを切り替える
- `code` は `ARTICLE_DUPLICATE_URL`、`VALIDATION_ERROR`、`AUTH_INVALID_CREDENTIALS` のような英大文字 snake case とし、UI 文言に依存しない機械判定に使う
- `fieldErrors` は validation error のときに `{ field, code, message }` を返す。`code` は `REQUIRED`、`SIZE`、`PATTERN`、`URL`、`MIN`、`MAX`、`INVALID` のいずれかを基本にする
- 例外から API エラーへ変換する責務は `ApiExceptionHandler` に集約する
- エラー種別の判定は例外 message の文字列ではなく、例外型または明示的な reason code で行う
- 想定済みの業務例外、認証例外、入力不正は下記の HTTP status へ変換し、内部例外 message をそのまま返さない
- バリデーションエラーや不正なパラメータは `400 Bad Request`
- 未認証または token 不正は `401 Unauthorized`
- CSRF token 不一致は `403 Forbidden`
- アクセスできない URL は `400 Bad Request`
- 存在しない記事 ID は `404 Not Found`
- 重複 URL は `409 Conflict`
- 重複タグ名、使用中タグの削除は `409 Conflict`
- 想定外例外は `500 Internal Server Error` とし、ログへ詳細を残しつつ API には汎用メッセージだけを返す
- 重複 URL の場合は、登録済み記事の詳細へ遷移できるよう `existingArticleId` を返す
- `status` の不正値、`id` の不正な UUID、`readDate` の不正な日付形式も `messages` 配列に説明文を入れて返す

例:

```json
{
  "timestamp": "2026-05-10T00:00:00Z",
  "code": "VALIDATION_ERROR",
  "messages": ["URL must be a valid URL."],
  "fieldErrors": [
    {
      "field": "url",
      "code": "URL",
      "message": "URL must be a valid URL."
    }
  ],
  "existingArticleId": null
}
```
