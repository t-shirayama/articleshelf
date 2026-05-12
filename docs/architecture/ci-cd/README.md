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
- `chrome-extension-release.yml`
  - `v*` タグ push または `workflow_dispatch` で `chrome-extension` を build し、`articleshelf-chrome-extension.zip` を GitHub Release asset として公開する
  - 拡張機能の配布仕様は [Chrome 拡張機能仕様](../../specs/features/browser-extension.md) を正本とする

## 3. 品質ゲート

- ローカル hook は CI を置き換えず、早期検知だけを担う。`.githooks/pre-commit` は軽量な静的ガード、`.githooks/pre-push` は変更パスに応じた targeted verification を実行し、最終判定は GitHub Actions の各 job を正本にする
- backend は Docker 経由で Maven を実行し、ローカル Maven の有無に依存しない
- `backend-check` は `docker compose run --rm backend mvn test spotbugs:check` を実行し、コンパイル不能、JUnit / PostgreSQL IT 失敗、Clean Architecture dependency test 違反、SpotBugs 警告を早い段階で止める
- backend の port / repository signature を変える変更では、domain から application DTO / query / command を import していないことを `CleanArchitectureDependencyTest` と review の両方で確認する
- backend の domain / application / architecture test 変更では `.githooks/pre-push` が `docker compose run --rm backend mvn test -Dtest=CleanArchitectureDependencyTest` を先行実行し、repository / persistence / adapter / test 変更では `docker compose run --rm backend mvn test spotbugs:check` を push 前に再現する
- `backend-unit` は JaCoCo coverage 付きで domain / application 中心の UT を実行し、domain / application line coverage 80% 未満を失敗にする
- `backend-integration` は Spring Boot と PostgreSQL 実体で認証境界、Repository 検索、DB 制約、JPA validate を確認する
- Java / Spring Boot / Mockito / JaCoCo / Surefire / SpotBugs 更新時は、`backend-check` の結果で Mockito javaagent、JaCoCo `argLine`、JVM warning が再発していないことを確認する
- frontend は ESLint、型チェック、Vite build、bundle size check を CI の早い段階で確認する
- `frontend-unit` は Vitest coverage threshold を適用し、global coverage では lines 43%、statements 42%、functions 35%、branches 29% 未満を失敗にする
- focused frontend coverage は `npm run test:unit:coverage:focused` で article domain helper、主要 composable、Markdown / API / JWT utility の高密度テストを確認し、lines / statements 85%、functions 90%、branches 70% 未満を失敗にする
- frontend の E2E 基盤、auth / router、workspace account、article composable、mobile shell / style 変更では `.githooks/pre-push` が targeted unit test と 2 本の Playwright smoke を必要に応じて実行し、CI の `frontend-unit` / `e2e` 失敗を push 前に減らす
- E2E はフロントエンド、バックエンド、DB を Compose 経由で起動し、主要導線と認証後記事一覧の axe accessibility scan の破壊を検知する
- Dependency Review は依存追加・更新 PR で moderate 以上の既知脆弱性を検知する。GitHub 側の Dependency graph が無効な repository では action が実行できないため、Dependency graph を有効化し、repository variable `DEPENDENCY_REVIEW_ENABLED=true` を設定した環境だけで必須 gate として実行する
- CodeQL は Java / JavaScript / TypeScript の security query を code scanning として実行する
- Trivy は repository filesystem の dependency / secret / misconfiguration と backend Docker image の high / critical vulnerability を検知する
- GitHub-owned actions は major tag、third-party actions は version tag を使い、`.github/dependabot.yml` の GitHub Actions 更新 PR で追従する。Dependabot の更新 PR は `develop` 宛てに作成し、`main` への直接反映を避ける。SHA pinning は運用負荷と Dependabot 追従性のバランスを見て、外部公開規模が上がった段階で再検討する

## 4. デプロイとの関係

CI 失敗時もデプロイ連携そのものは停止しないため、main 反映前のレビューと CI 成功確認を運用で徹底する。
公開構成、環境変数、デプロイ運用の詳細は [デプロイ構成](../../deployment/README.md) に従う。
