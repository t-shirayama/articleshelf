# Test Data

## 1. 基本方針

- 通常起動や build では記事データを自動投入しない
- テストは既存データに依存せず、ケースごとに一意なデータを作成する
- 認証ユーザーはケースごとに一意な username を使う
- 外部サイトの OGP 取得安定性にテストを依存させない

## 2. Unit Test

- 外部 I/O を伴う入力は test double や固定値へ置き換える
- 時刻や認証状態など結果に影響する値は、テスト内で明示的に固定する

## 3. Integration Test

- IT の OGP 取得は application port を test double で差し替える
- API 境界では request / response JSON、validation、例外変換、認証境界を確認する
- DB 方言差が重要なケースでは PostgreSQL 実体を使う

## 4. E2E Test

- E2E ではテストごとに一意 username / URL を使い、既存データに依存しない
- E2E の URL は `https://example.com/?articleshelfE2e=...` のようにケースごとに一意にする
- 管理者リセット E2E だけ `docker-compose.e2e.yml` の `ARTICLESHELF_INITIAL_USER_ENABLED=true` で初期 ADMIN を明示作成する

## 5. 初期 ADMIN

- 通常起動では初期ユーザー、記事、タグを自動投入しない
- 初期 ADMIN が必要な検証環境だけ `ARTICLESHELF_INITIAL_USER_ENABLED=true` を指定する
- E2E 用 Compose は管理者リセットシナリオのために初期 ADMIN を明示的に有効化する
