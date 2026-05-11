# CI / CD Architecture

ArticleShelf の CI / CD は GitHub Actions を中心に、品質確認と公開環境への反映を分けて扱う。
テストの目的、スコープ、ケース、実行方法は [テスト戦略](../../testing/README.md) に従う。

## 1. CI の段階構成

現在の `.github/workflows/ci.yml` は次の段階で実行する。

| Step | Job | 主な責務 |
| --- | --- | --- |
| 1 | `backend-check` / `frontend-check` | コンパイル、型チェック、静的解析、bundle size、アーキテクチャ依存関係の確認 |
| 2 | `backend-unit` / `frontend-unit` | backend domain / application UT と frontend UT を coverage 付きで実行 |
| 3 | `backend-integration` / `frontend-integration` | Spring Boot / PostgreSQL IT と frontend integration test を実行 |
| 4 | `e2e` | Playwright Chromium で P0 導線を確認 |

`main` / `develop` では全ジョブを実行し、それ以外のブランチでは変更パスに応じて backend / frontend / E2E の関連ジョブだけを実行する。

## 2. Workflow 構成

- `ci.yml`
  - pull request / push で frontend build、backend check、unit、integration、E2E を段階実行する
  - P0 E2E も同じ workflow の `e2e` job として実行する
- `codeql.yml`
  - pull request / `main` / `develop` push / weekly schedule で Java / JavaScript / TypeScript の CodeQL analysis を実行する
- `supply-chain.yml`
  - pull request では、repository の Dependency graph と `DEPENDENCY_REVIEW_ENABLED=true` が有効な場合に Dependency Review で lockfile / dependency 差分を確認する
  - pull request / `main` / `develop` push / weekly schedule で Trivy filesystem scan と backend Docker image scan を実行する

## 3. 品質ゲート

- backend は Docker 経由で Maven を実行し、ローカル Maven の有無に依存しない
- `backend-check` は compile、SpotBugs、Clean Architecture dependency test を実行し、コンパイル不能、静的解析違反、レイヤー依存違反を早い段階で止める
- `backend-unit` は JaCoCo coverage 付きで domain / application 中心の UT を実行し、domain / application line coverage 80% 未満を失敗にする
- `backend-integration` は Spring Boot と PostgreSQL 実体で認証境界、Repository 検索、DB 制約、JPA validate を確認する
- frontend は ESLint、型チェック、Vite build、bundle size check を CI の早い段階で確認する
- `frontend-unit` は Vitest coverage threshold を適用し、lines 19%、statements 19%、functions 14%、branches 16% 未満を失敗にする
- E2E はフロントエンド、バックエンド、DB を Compose 経由で起動し、主要導線と認証後記事一覧の axe accessibility scan の破壊を検知する
- Dependency Review は依存追加・更新 PR で moderate 以上の既知脆弱性を検知する。GitHub 側の Dependency graph が無効な repository では action が実行できないため、Dependency graph を有効化し、repository variable `DEPENDENCY_REVIEW_ENABLED=true` を設定した環境だけで必須 gate として実行する
- CodeQL は Java / JavaScript / TypeScript の security query を code scanning として実行する
- Trivy は repository filesystem の dependency / secret / misconfiguration と backend Docker image の high / critical vulnerability を検知する
- GitHub-owned actions は major tag、third-party actions は version tag を使い、`.github/dependabot.yml` の GitHub Actions 更新 PR で追従する。Dependabot の更新 PR は `develop` 宛てに作成し、`main` への直接反映を避ける。SHA pinning は運用負荷と Dependabot 追従性のバランスを見て、外部公開規模が上がった段階で再検討する

## 4. デプロイとの関係

CI 失敗時もデプロイ連携そのものは停止しないため、main 反映前のレビューと CI 成功確認を運用で徹底する。
公開構成、環境変数、デプロイ運用の詳細は [デプロイ構成](../../deployment/README.md) に従う。
