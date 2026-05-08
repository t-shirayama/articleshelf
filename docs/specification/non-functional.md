# 非機能仕様

ReadStack の品質属性と運用上守るべき仕様を定義する。
採用技術、推奨バージョン、開発環境は [technology.md](technology.md) に分ける。

## 1. セキュリティ

- 記事、タグ、メモ、既読履歴は認証ユーザーの `user_id` で必ずスコープする
- API は protected endpoint で `Authorization: Bearer <accessToken>` を必須にする
- access token は短命 JWT とし、発行 / 検証は Spring Security JOSE に委譲する
- refresh token は HttpOnly cookie として扱い、DB には HMAC-SHA256 hash のみ保存する
- production profile では `AUTH_CSRF_ENABLED=true` を必須にする
- frontend と API が別 site の公開構成では `SameSite=None; Secure` と CSRF token を必須にする
- `FRONTEND_ORIGIN` は明示的な origin を指定し、CORS で `*` は使わない
- Markdown 表示は raw HTML を無効化し、DOMPurify でサニタイズする
- 詳細な認証、Cookie、CSRF、CORS 仕様は [authentication.md](authentication.md) に従う

## 2. 入力検証とエラー応答

- バリデーションはフロントエンドとバックエンドの両方で実施する
- フロントエンドは即時フィードバックと誤操作防止を担当する
- バックエンドは API 契約と永続化前の最終防衛線として必ず検証する
- 認証失敗、権限不足、他ユーザーデータ参照、入力不正、重複、想定外エラーは共通ルールで HTTP status と error body に変換する
- 想定外エラーでは内部実装の詳細をレスポンスに含めない

## 3. 信頼性とデータ保護

- 永続化 DB は PostgreSQL を前提にする
- DB schema は Flyway migration で管理し、JPA は schema validation を行う
- application 層の user scope 検証に加え、DB の unique 制約や複合 FK で user mismatch を拒否する
- refresh token は rotation し、失効済み token の再利用時は同一 family を失効する
- managed PostgreSQL を使う公開構成では JDBC URL 側で TLS を有効化する

## 4. 性能と UX 安定性

- OGP画像本体はフロントエンドの IndexedDB に Blob としてキャッシュする
- OGP取得失敗URLは一定時間再試行を抑制し、同じ失敗取得を繰り返さない
- 記事一覧、カレンダー、タグ管理などスクロール領域は画面全体ではなく必要な領域に限定する
- スクロールバーの有無や閲覧 / 編集切り替えでカード幅や本文領域幅が変わらないようにする
- 長い日本語 / 英語文言、タグ名、URL が UI の主要操作を押し出さないようにする

## 5. 保守性

- バックエンドは DDD / クリーンアーキテクチャの依存方向を守る
- `CleanArchitectureDependencyTest` で domain / application / adapter / infrastructure の依存関係を CI で検査する
- フロントエンドは feature-oriented 構成を維持し、API 通信、store、composable、domain helper、UI component の責務を分ける
- API 契約、画面仕様、テスト方針、運用ルールを変更した場合は関連 docs を同じ作業で更新する

## 6. テストと CI

- P0 中心の単体テスト、結合テスト、E2E テストを整備する
- backend / frontend の check、unit、integration、E2E は CI で段階実行する
- backend unit と frontend unit は coverage を確認する
- backend unit coverage は JaCoCo CSV を使い、domain / application 層の line coverage が 55% 未満なら CI を失敗させる。初期値は緩めに置き、対象テスト拡充後に段階的に引き上げる
- Spring Data JPA の `@Query`、JPQL、native SQL、DB 制約を変更した場合は PostgreSQL 実体を使う integration test で確認する
- テスト範囲、具体的なコマンド、CI の job 構成は [docs/testing/README.md](../testing/README.md) に従う
