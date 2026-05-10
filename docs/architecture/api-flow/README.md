# API And Client Flow

## 認証フロー

- 未ログイン時は `AuthScreen` を表示し、`POST /api/auth/login` または `POST /api/auth/register` で access token と refresh cookie を受け取る
- access token は Pinia の `features/auth` store にメモリ保持し、API client が `Authorization: Bearer` を付与する
- ページ再読み込み時は `POST /api/auth/refresh` で session を復元する
- access token の期限前 refresh と、`401` 時の refresh / retry を API client が担当する

## 記事 / タグ API フロー

- フロントエンドは `GET /api/articles` で一覧を取得
- 初回取得では検索、ステータス、単一タグ、お気に入り条件をクエリパラメータで送信でき、バックエンドは Repository / JPA クエリで user scoped に絞り込む
- 複数タグ、おすすめ度、登録日範囲、既読日範囲、並び替えは取得後にフロントエンド側の Pinia store で適用する
- `POST /api/articles` で記事を追加
- `PUT /api/articles/{id}` で記事を更新
- 記事一覧カードの未読 / 既読切り替えとお気に入り切り替えは、フロントエンドで楽観的に反映してから `PUT /api/articles/{id}` で保存する
- `GET /api/tags` でタグ一覧を取得
- `POST /api/tags` でタグを追加

## OGP 画像フロー

- OGP画像はDB上の `thumbnail_url` を直接表示せず、記事カードのサムネイル領域が表示範囲に近づいた時だけフロントエンドが取得する
- 取得したサムネイル Blob は IndexedDB に保存し、最大 200 records または 50MB を超える場合は `cachedAt` / `failedAt` が古いものから削除する
- CORS 非対応、非画像、5MB 超過、取得失敗のサムネイルは proxy せず、安全側にプレースホルダー表示へ倒す。失敗記録は 24 時間 retry を抑制し、期限切れ後は eviction 対象にする
- OGP HTML は `OgpHtmlParser` が meta tag / title 抽出を担当し、`Content-Type` charset、meta charset、UTF-8 fallback の順で文字コードを決定する
- 取得した画像は IndexedDB に画像 Blob として保存したものを表示する
- 画像取得に失敗したURLは一定時間再試行せず、外部サイトへのアクセス増加を避けてプレースホルダー表示に戻す
