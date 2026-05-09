# Test CI

## 1. 現在の CI

現在の `.github/workflows/ci.yml` は次を実行する。

- Step 1: `backend-check` / `frontend-check`
  - backend は `docker compose run --rm backend mvn clean compile spotbugs:check` と `docker compose run --rm backend mvn test -Dtest=CleanArchitectureDependencyTest` でコンパイル、SpotBugs、依存方向を確認する
  - frontend は `npm run typecheck` と `npm run build` で型チェックと Vite ビルドを確認する
- Step 2: `backend-unit` / `frontend-unit`
  - backend の domain / application UT と frontend の Vitest UT を coverage 付きで実行する
  - backend は JaCoCo CSV から instruction / branch / line coverage summary を出力し、domain / application line coverage 80% 未満を失敗にする
  - frontend は Vitest coverage-v8 の text summary を出力する
- Step 3: `backend-integration` / `frontend-integration`
  - backend の Spring Boot / PostgreSQL IT と frontend の `*.integration.test.ts` を分けて実行する
- Step 4: `e2e`
  - Playwright Chromium で P0 導線を確認する

`main` / `develop` では全ジョブを実行し、それ以外のブランチでは変更パスに応じて backend / frontend / E2E の関連ジョブだけを実行する。

## 2. Workflow 構成

- `ci.yml`
  - pull request / push で frontend build, backend test, unit test を実行
- `e2e.yml`
  - pull request / main push で P0 E2E を実行

## 3. 完了条件

リリース前の最小完了条件は達成済み。現在の基準は次の通り。

- P0 UT が CI で実行される: 達成
- P0 IT が CI で実行される: 達成
- P0 E2E が main push 前後で実行できる: 達成
- 認証追加後、ユーザー A がユーザー B の記事を参照・更新・削除できないことを IT / E2E で確認する: 達成
- DB 初期化、テストデータ、stub の運用が文書化されている: 達成。通常起動では自動投入せず、テスト用一意データで確認する
- CI 失敗時もデプロイ連携そのものは停止しないため、main 反映前のレビューと CI 成功確認を運用で徹底する
