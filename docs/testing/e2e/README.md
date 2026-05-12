# E2E Test

## 1. 目的

E2E は、ユーザーがブラウザで使う主要導線を、フロントエンド、バックエンド、DB を結合して検証する。
経営説明では「リリース前後の代表操作は自動で担保している」と示せるテスト群にする。

## 2. スコープ

- Vite frontend
- Spring Boot backend
- PostgreSQL
- Playwright による Chromium 実行
- デスクトップ幅の代表確認
- スマホ幅の代表確認は Playwright の `mobile` project で実行する
- username 登録、ログイン、ログアウト、アカウント操作、ユーザースコープ

## 3. 対象外

- 外部サイトの OGP 安定性
- アプリ外運用で行う管理者リセット後の本人通知
- 本番インフラ障害
- すべてのブラウザ組み合わせ
- CSS の完全一致

## 4. Playwright 実行方針

推奨 npm script:

```json
{
  "scripts": {
    "test:e2e": "playwright test",
    "test:e2e:ui": "playwright test --ui"
  }
}
```

現行 `frontend/package.json` には次の script を追加済み。

```json
{
  "scripts": {
    "test:unit": "vitest run",
    "test:e2e": "playwright test",
    "test:e2e:ui": "playwright test --ui"
  }
}
```

推奨構成:

- `frontend/e2e/` に spec を配置
- `playwright.config.ts` で `desktop` project と `mobile` project を定義する
- `desktop` project は通常の認証済み記事フロー、`mobile` project は `mobile-responsive.spec.ts` を担当する
- `webServer` は `docker compose up --build` を前面起動し、frontend / backend / PostgreSQL をまとめて起動する
- Compose は backend の `/actuator/health` healthcheck が healthy になってから frontend を起動し、登録直後の API 接続失敗を避ける
- CI では既存サーバーを再利用せず、Playwright が起動した Docker Compose プロセスを監視する
- ローカルでも既定では既存 `localhost:5173` / `:8080` を再利用せず、`docker-compose.e2e.yml` の専用 stack を `localhost:4173` / `:18080` で毎回起動する
- 既存サーバーを再利用するのは、`PLAYWRIGHT_USE_EXISTING_SERVER=1` を明示し、そのサーバーが `docker-compose.e2e.yml` と同じ auth / initial user / rate limit 前提で起動していると確認できる場合だけにする
- `.githooks/pre-commit` では、E2E 基盤ファイル変更時に `frontend/playwright.config.ts` の `PLAYWRIGHT_USE_EXISTING_SERVER === '1'` と `reuseExistingServer: false` を静的確認し、専用 stack 既定の逸脱を commit 前に止める
- 初期 ADMIN `owner` や `ARTICLESHELF_AUTH_RATE_LIMIT_ENABLED=false` のような E2E 前提は専用 stack に含め、通常開発用 compose と混同しない
- 同じ `articleshelf-e2e` compose project を使う Playwright 実行は並列に 2 本以上起動しない。`npm run test:e2e` の 1 プロセスでまとめて実行し、個別確認も 1 コマンドずつ直列で回す
- スクリーンショット比較は初期段階では必須にせず、主要表示が非空で操作可能なことを優先する

## 5. 動作確認方法

- 通常実行: `cd frontend && npm run test:e2e`
- UI mode: `cd frontend && npm run test:e2e:ui`
- E2E 用 Compose は `docker-compose.e2e.yml` の設定を使い、各テストは一意データを作成する
- E2E 専用 stack は通常開発用 compose との port 競合を避けるため、frontend `4173` / backend `18080` を使う
- 既存サーバーを意図的に使う場合だけ `PLAYWRIGHT_USE_EXISTING_SERVER=1 npm run test:e2e` を使う

## 6. テストデータ

- E2E は既存データに依存せず、テストごとに一意 username / URL を使う
- 記事追加 URL は `https://example.com/?articleshelfE2e=...` のようにケースごとに一意にする
- 外部サイトの OGP 安定性に依存しすぎないよう、E2E では実外部サイト固有の OGP 取得結果を前提にしない
- 管理者リセット E2E だけ `docker-compose.e2e.yml` の `ARTICLESHELF_INITIAL_USER_ENABLED=true` で初期 ADMIN を明示作成する
- 通常起動時の初期 ADMIN と初期データの仕様は [認証仕様](../../specs/auth/README.md) と [Runtime Architecture](../../architecture/runtime/README.md) を正本にする

## 7. CI での扱い

- `e2e` job で Playwright Chromium による P0 導線を確認する
- CI では既存サーバーを再利用せず、Playwright が起動した Docker Compose プロセスを監視する
- `main` / `develop` では全体確認の一部として実行し、それ以外のブランチでは変更パスに応じて関連ジョブとして実行する

## 8. ルール

- E2E は主要導線の代表シナリオに絞る
- 細かな分岐やエラー詳細は Unit / Integration Test へ寄せる
- 外部サイトの OGP 安定性に依存しない URL を使う
- `owner` ログイン、初期 ADMIN、認証 rate limit 無効化のような前提を持つ spec では、通常開発用 compose や手元の既存サーバーに暗黙依存しない
- auth / router / workspace account / E2E 基盤の変更では `.githooks/pre-push` が targeted unit test と authenticated E2E smoke を走らせるため、ローカルでもこの hook を有効にした状態で運用する
- ケース一覧と実装済みテストは [E2E Test ケース](cases.md) に記録する
