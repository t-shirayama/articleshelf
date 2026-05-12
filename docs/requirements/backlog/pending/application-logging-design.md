# Application logging design

## 状態

未対応

## 優先度

P2

## 目的

ArticleShelf の障害調査、CI / E2E 失敗時の切り分け、本番運用時の原因追跡をしやすくするため、ログ機能の方針を設計から整理する。
実装前に、何を運用ログとして残し、何を監査ログまたは将来機能として分けるかを明確にする。

## 対象

- backend の request / response / exception / auth / OGP 取得ログ
- frontend のユーザー操作、API error、想定外例外の扱い
- `X-Request-Id`、backend logging MDC、API error response との関係
- secret、password、refresh token、access token、個人データをログへ出さないためのルール
- `docs/specs/quality/README.md`
- `docs/specs/security/README.md`
- `docs/architecture/backend/README.md`
- `docs/testing/README.md`

## 対応内容

- まず設計ドキュメントで、運用ログ、セキュリティ監査ログ、ユーザー向け履歴機能の境界を決める。
- backend は requestId、userId または匿名状態、method、path、status、duration、例外種別を構造化して追える方針を整理する。
- frontend は API error、画面遷移、重要操作のログ対象と、ブラウザ console / 外部収集 / backend 送信の採用可否を整理する。
- 認証、記事作成 / 更新 / 削除、タグ操作、OGP 取得失敗など、調査価値が高いイベントを分類する。
- password、token、cookie、CSRF token、記事本文、メモ本文など、ログ禁止またはマスキング対象を明文化する。
- ローカル開発、CI、公開環境でログレベルと出力先をどう切り替えるか決める。
- 設計が固まった後、必要な backend / frontend 実装タスクに分割する。

## 完了条件

- ログ機能の設計方針が specs / architecture / testing docs に反映される。
- 運用ログ、監査ログ、ユーザー向け履歴機能を混同しない責務境界が決まっている。
- requestId を使って frontend の失敗、backend request、backend exception を追跡できる方針がある。
- 機密情報と個人データのログ出力禁止ルール、マスキング方針、検証観点が明文化されている。
- 実装タスクへ分割できる粒度で、対象イベント、ログレベル、出力先、テスト観点が整理されている。

## 根拠

CI の E2E タイムアウトや backend / frontend 起動順序の問題を調査する際、Compose 状態、backend health、frontend の API 接続失敗、request 単位の追跡情報を横断して見る必要があった。
現在は一部の quality spec に `X-Request-Id` と MDC の記載があるものの、ログ機能全体としての対象、粒度、禁止情報、運用環境ごとの出力方針はまとまっていない。
