# CI / CD Architecture

ArticleShelf の CI / CD は GitHub Actions を中心に、品質確認と公開環境への反映を分けて扱う。
テストの目的、スコープ、ケース、実行方法は [テスト戦略](../../testing/README.md) に従う。

## 1. CI の段階構成

現在の `.github/workflows/ci.yml` は次の段階で実行する。

| Step | Job | 主な責務 |
| --- | --- | --- |
| 1 | `backend-check` / `frontend-check` | コンパイル、型チェック、静的解析、アーキテクチャ依存関係の確認 |
| 2 | `backend-unit` / `frontend-unit` | backend domain / application UT と frontend UT を coverage 付きで実行 |
| 3 | `backend-integration` / `frontend-integration` | Spring Boot / PostgreSQL IT と frontend integration test を実行 |
| 4 | `e2e` | Playwright Chromium で P0 導線を確認 |

`main` / `develop` では全ジョブを実行し、それ以外のブランチでは変更パスに応じて backend / frontend / E2E の関連ジョブだけを実行する。

## 2. Workflow 構成

- `ci.yml`
  - pull request / push で frontend build、backend check、unit、integration、E2E を段階実行する
  - P0 E2E も同じ workflow の `e2e` job として実行する

## 3. 品質ゲート

- backend は Docker 経由で Maven を実行し、ローカル Maven の有無に依存しない
- frontend は型チェックと Vite build を CI の早い段階で確認する
- backend coverage は JaCoCo CSV から domain / application 層の line coverage を集計し、80% 未満なら失敗させる
- E2E はフロントエンド、バックエンド、DB を Compose 経由で起動し、主要導線の破壊を検知する

## 4. デプロイとの関係

CI 失敗時もデプロイ連携そのものは停止しないため、main 反映前のレビューと CI 成功確認を運用で徹底する。
公開構成、環境変数、デプロイ運用の詳細は [デプロイ構成](../../deployment/README.md) に従う。
