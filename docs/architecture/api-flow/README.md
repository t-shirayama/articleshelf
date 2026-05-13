# API And Client Flow

## 認証フロー

- 未ログイン時は `AuthScreen` を表示し、`POST /api/auth/login` または `POST /api/auth/register` で access token と refresh cookie を受け取る
- access token は Pinia の `features/auth` store にメモリ保持し、API client が `Authorization: Bearer` を付与する
- ページ再読み込み時は `POST /api/auth/refresh` で session を復元する
- access token の期限前 refresh と、`401` 時の refresh / retry を API client が担当する

## 記事 / タグ API フロー

- フロントエンドは `GET /api/articles` で一覧を取得
- 記事一覧では検索、ステータス、複数タグ OR、お気に入り、複数おすすめ度、登録日範囲、既読日範囲、並び順、ページ番号をクエリパラメータで送信し、バックエンドは Repository / JPA クエリで user scoped に絞り込む
- `page` / `size` / `sort` は backend の repository 実装が PostgreSQL の `LIMIT` / `OFFSET` / `ORDER BY` に変換し、同一値時は API 契約で定めた tie-breaker を使う
- カレンダー、タグ管理、サイドバー件数は current page 一覧とは別に全件 snapshot を取得し、一覧の正本と互換用途を分ける
- `POST /api/articles` で記事を追加
- `PUT /api/articles/{id}` で記事を更新
- 記事詳細の `GET /api/articles/{id}` と一覧レスポンスは optimistic locking 用 `version` を返し、frontend は更新時にその `version` を同送する
- 記事追加 / URL 変更を伴う更新では、URL 重複確認と保存だけを短い transaction にし、外部 HTTP アクセスである OGP 取得は DB transaction 外で同期実行する
- 記事更新では、まず current article と client version を照合し、競合していれば `409 ARTICLE_VERSION_CONFLICT` を返して外部 OGP fetch と保存 transaction を開始しない
- 記事一覧カードの未読 / 既読切り替えとお気に入り切り替えは、フロントエンドで楽観的に反映してから `PUT /api/articles/{id}` で保存する
- `GET /api/tags` でタグ一覧を取得
- `POST /api/tags` でタグを追加

## OGP 画像フロー

- OGP画像はDB上の `thumbnail_url` を直接表示せず、記事カードのサムネイル領域が表示範囲に近づいた時だけフロントエンドが取得する
- 取得したサムネイル Blob は IndexedDB に保存し、最大 200 records または 50MB を超える場合は `cachedAt` / `failedAt` が古いものから削除する
- CORS 非対応、非画像、5MB 超過、取得失敗のサムネイルは proxy せず、安全側にプレースホルダー表示へ倒す。失敗記録は 24 時間 retry を抑制し、期限切れ後は eviction 対象にする
- OGP HTML は `OgpHtmlParser` が meta tag / title 抽出を担当し、`Content-Type` charset、meta charset、UTF-8 fallback の順で文字コードを決定する
- OGP 取得失敗、timeout、SSRF guard 失敗は記事追加時または URL 変更時に `ArticleUrlUnavailableException` として扱い、DB 保存 transaction を開始しない
- 取得した画像は IndexedDB に画像 Blob として保存したものを表示する
- 画像取得に失敗したURLは一定時間再試行せず、外部サイトへのアクセス増加を避けてプレースホルダー表示に戻す
