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
- ローカルでは既に `docker compose up --build` 済みのサーバーがあれば再利用する
- スクリーンショット比較は初期段階では必須にせず、主要表示が非空で操作可能なことを優先する

## 5. 動作確認方法

- 通常実行: `cd frontend && npm run test:e2e`
- UI mode: `cd frontend && npm run test:e2e:ui`
- E2E 用 Compose は `docker-compose.e2e.yml` の設定を使い、各テストは一意データを作成する

## 6. テストデータ

- E2E は既存データに依存せず、テストごとに一意 username / URL を使う
- 記事追加 URL は `https://example.com/?articleshelfE2e=...` のようにケースごとに一意にする
- 外部サイトの OGP 安定性に依存しすぎないよう、E2E では実外部サイト固有の OGP 取得結果を前提にしない
- 管理者リセット E2E だけ `docker-compose.e2e.yml` の `ARTICLESHELF_INITIAL_USER_ENABLED=true` で初期 ADMIN を明示作成する
- 通常起動時の初期 ADMIN と初期データの仕様は [認証仕様](../../specification/auth/README.md) と [Runtime Architecture](../../architecture/runtime/README.md) を正本にする

## 7. CI での扱い

- `e2e` job で Playwright Chromium による P0 導線を確認する
- CI では既存サーバーを再利用せず、Playwright が起動した Docker Compose プロセスを監視する
- `main` / `develop` では全体確認の一部として実行し、それ以外のブランチでは変更パスに応じて関連ジョブとして実行する

## 8. ルール

- E2E は主要導線の代表シナリオに絞る
- 細かな分岐やエラー詳細は Unit / Integration Test へ寄せる
- 外部サイトの OGP 安定性に依存しない URL を使う
- ケース一覧と実装済みテストは [E2E Test ケース](cases.md) に記録する
