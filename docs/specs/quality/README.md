# 非機能仕様

ArticleShelf の品質属性と運用上守るべき仕様を定義する。
採用技術、推奨バージョン、開発環境は [../../architecture/technology/README.md](../../architecture/technology/README.md) に分ける。

## 1. セキュリティ

- セキュリティ要件は [セキュリティの非機能要件](../../requirements/non-functional/security.md) に従う
- 認証、認可、CSRF/CORS、secret、rate limit、SSRF、Markdown sanitization の具体仕様は [セキュリティ仕様](../security/README.md) と [認証仕様](../auth/README.md) を正本とする

## 2. 入力検証とエラー応答

- バリデーションはフロントエンドとバックエンドの両方で実施する
- フロントエンドは即時フィードバックと誤操作防止を担当する
- バックエンドは API 契約と永続化前の最終防衛線として必ず検証する
- 認証失敗、権限不足、他ユーザーデータ参照、入力不正、重複、想定外エラーは共通ルールで HTTP status と error body に変換する
- 想定外エラーでは内部実装の詳細をレスポンスに含めない

## 3. 信頼性とデータ保護

- 永続化 DB は PostgreSQL を前提にする
- DB schema、user scope、複合 FK、refresh token のデータ構造は [データモデル](../data/README.md) を正本とする
- 永続化方針と schema 管理は [Data Architecture](../../architecture/data/README.md) に従う
- managed PostgreSQL を使う公開構成では JDBC URL 側で TLS を有効化する

## 4. 性能と UX 安定性

- OGP画像本体はフロントエンドの IndexedDB に Blob としてキャッシュする
- OGP取得失敗URLは一定時間再試行を抑制し、同じ失敗取得を繰り返さない
- UI のスクロール領域、幅の安定性、長い文言の収まりは [UI仕様](../ui/README.md) と [デザイン文書](../../designs/README.md) を正本とする

## 5. 保守性

- バックエンドは DDD / クリーンアーキテクチャの依存方向を守る
- `CleanArchitectureDependencyTest` で domain / application / adapter / infrastructure の依存関係を CI で検査する
- フロントエンドは feature-oriented 構成を維持し、API 通信、store、composable、domain helper、UI component の責務を分ける
- API 契約、画面仕様、テスト方針、運用ルールを変更した場合は関連 docs を同じ作業で更新する

## 6. テストと CI

- P0 中心の単体テスト、結合テスト、E2E テストを整備する
- backend / frontend の check、unit、integration、E2E は CI で段階実行する
- backend unit と frontend unit は coverage を確認する
- テスト範囲、具体的なコマンド、CI の job 構成は [テスト戦略](../../testing/README.md) に従う

## 7. 観測性

- backend は request ごとに `X-Request-Id` を受け取り、未指定時は UUID を生成して response header と logging MDC の `requestId` に設定する
- 運用ログは request / response / exception / auth / OGP 取得の調査に必要な最小限の事実だけを構造化して残し、ユーザー向け履歴機能や将来の監査ログとは責務を分ける
- backend の構造化ログは `requestId`、`method`、`path`、`status`、`durationMs`、`outcome`、`exceptionType`、認証済み user の内部識別子または anonymous 状態を基本項目にする
- frontend は browser console を恒久的な記録先にせず、開発時の補助と割り切る。本番で継続収集する対象は API error、画面遷移失敗、想定外例外、重要操作失敗に限定し、`X-Request-Id` を使って backend 側の request / exception と突き合わせられる形を優先する
- 認証失敗、rate limit、記事作成 / 更新 / 削除、タグ操作、OGP 取得失敗は調査価値が高いイベントとして、metrics だけで不足する場合は構造化ログでも追跡できるようにする
- request / response body 全文、password、access token、refresh token、CSRF token、Cookie 値、記事本文、メモ本文、検索語全文、外部 URL の query 全文は運用ログへ出さない
- 個人データや本文を含む可能性がある値は allowlist 方式で扱い、必要最小限の ID、件数、結果種別、長さ、boolean 状態だけを記録する
- ログレベルは local development ではデバッグしやすさ、CI では失敗原因追跡、本番では安定運用を優先し、既定値は `INFO`、異常系の詳細は `WARN` / `ERROR` に寄せる
- metrics は Actuator の `/actuator/metrics` で確認できる範囲に限定し、token、username、URL、メモ本文などの個人情報や secret を tag / value に含めない
- OGP 取得は `articleshelf.ogp.fetch` timer で `accessible`、`unavailable`、`invalid_input`、`error` の outcome を記録する
- 認証失敗は `articleshelf.auth.failure`、rate limit 超過は `articleshelf.auth.rate_limited` で理由や operation のみを記録する
- access token の拒否は `articleshelf.auth.access_token_rejected` で理由のみを記録し、token 値、username、IP は metrics tag に含めない
- 記事作成 / 更新は `articleshelf.article.created`、`articleshelf.article.updated` counter で件数だけを記録する
