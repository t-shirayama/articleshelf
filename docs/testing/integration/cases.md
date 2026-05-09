# Integration Test Cases

## 1. IT ケース一覧

| ID | 優先度 | 対象 | 観点 | 期待結果 |
| --- | --- | --- | --- | --- |
| IT-API-001 | P0 | `POST /api/articles` | 正常登録 | `200` または `201`、保存値が返る |
| IT-API-002 | P0 | `POST /api/articles` | URL 未入力 | `400`、`messages` を返す |
| IT-API-003 | P0 | `POST /api/articles` | 重複 URL | `409`、`existingArticleId` を返す |
| IT-API-004 | P0 | `GET /api/articles/{id}` | 存在しない ID | `404` |
| IT-API-005 | P0 | `PUT /api/articles/{id}` | タグ置換 | 古い関連が消え、新しい関連になる |
| IT-API-006 | P0 | `DELETE /api/articles/{id}` | 削除 | 以降の詳細取得が `404` |
| IT-API-007 | P1 | `GET /api/articles` | 検索 | タイトル、URL、概要、メモに一致 |
| IT-API-008 | P1 | `GET /api/articles` | タグ単一条件 | 大文字小文字を区別せず一致 |
| IT-API-009 | P1 | `GET /api/tags` | タグ一覧 | 名前昇順 |
| IT-API-010 | P1 | ArticleRequest validation | API 境界の入力制約 | `rating` 範囲外や長すぎる `title` を 400 で拒否する |
| IT-BE-006 | P1 | Auth rate limit API | 429 応答 | Spring が確定した client IP / remoteAddr を使い、register / login の超過時に統一 JSON エラーを返す |
| IT-DB-001 | P0 | ArticleEntity | `Article` 保存 | UUID、日付、enum、tag 関連が保持される |
| IT-DB-002 | P0 | unique URL | DB 制約 | 同一 URL が二重保存されない |
| IT-DB-003 | P1 | Repository 検索 | PostgreSQL 実体で `LIKE`、nullable parameter、JOIN を含む複合条件 | SQL 型推論エラーを起こさず条件一致のみ返す |
| IT-AUTH-001 | P0 | 認証追加後 | 未ログイン | 保護 API は `401` |
| IT-AUTH-002 | P0 | 認証追加後 | ユーザー A の token でユーザー B の記事 ID | `404` または `403` |
| IT-AUTH-003 | P0 | 認証追加後 | refresh token rotation | 旧 refresh token は再利用不可 |
| IT-OPS-001 | P0 | health check | DB 接続正常 | `2xx` |
| IT-OPS-002 | P1 | CORS | 許可 origin | 認証 cookie / header を送れる |

## 2. 実装済み IT

- `AuthAndArticleIntegrationTest`
  - 未認証の保護 API が `401` を返す
  - 登録ユーザーが記事を作成できる
  - ユーザー B の一覧・詳細からユーザー A の記事が見えない
  - URL 重複判定が user scoped で動く
  - malformed JSON、UUID / enum / boolean 型不正を `400` の統一エラー形式で返す
  - framework 由来の method not allowed を `405` のまま返す
  - タグ名重複、同一タグ統合、存在しないタグ、使用中タグ削除のエラー応答
  - refresh token rotation 後、旧 refresh token の再利用が `401` になる
  - 全端末ログアウト後、既発行 access token と refresh token がどちらも無効になる
