# Unit Test

## 1. 目的

UT は、外部 I/O に依存しない小さな単位で仕様を固定する。
主にドメインルール、変換処理、フィルタ・ソート、フォーム状態、API 入出力変換を対象にし、変更時にすぐ壊れた箇所を特定できる状態を作る。

## 2. スコープ

### バックエンド

- `domain`
  - `Article` の初期値、ステータス、日付、評価値、タグ正規化
  - `ArticleStatus` の扱い
  - ドメイン例外の発生条件
- `application`
  - `ArticleService` の記事追加、更新、削除、検索条件
  - OGP 取得結果の適用
  - 重複 URL 検出
  - アクセス不可 URL の拒否
  - 未読 / 既読切り替え時の既読日ルール
- `adapter`
  - request DTO から command への変換
  - error response の組み立て
- `infrastructure`
  - OGP HTML 解析を分離した場合の parser
  - JPA entity と domain の mapping は純粋関数化できる範囲を UT 化

### フロントエンド

- `features/articles/domain`
  - 検索、フィルタ、ソート、日付範囲判定
  - フォーム値から API input への変換
  - API response から表示モデルへの変換
- Pinia store
  - 記事一覧取得後の状態更新
  - 楽観的更新と失敗時 rollback
  - フィルタ条件、タイトル表示、カレンダー表示モード
- Markdown 表示 helper
  - raw HTML 無効化
  - DOMPurify に渡す前後の変換境界
  - 許可スキームの判定
- UI component
  - 重要な分岐表示のみ。詳細な見た目は E2E / visual capture へ寄せる

## 3. 対象外

- 実 DB への接続
- 実ブラウザ操作
- 外部サイトへの OGP 取得
- CSS のピクセル完全一致
- GitHub Actions や Render など外部サービスの実通信

## 4. 実装方針

- 外部 I/O、時刻、API client、OGP 取得、認証状態などは test double や固定入力へ寄せる
- ドメイン・application の重要分岐は、UI や API を介さず小さな単位で固定する
- フロントエンドの store / composable は成功系だけでなく、通信失敗や rollback を確認する
- セキュリティ境界に関わる pure logic は UT で高速に固定し、API 境界は Integration Test で確認する

## 5. 実行方法

テストツールの採用一覧は [技術スタック](../../architecture/technology.md) に集約する。
現行実装では `frontend/package.json` の `npm run test:unit` でフロントエンド UT を実行する。
coverage 確認は `npm run test:unit:coverage` で実行し、text summary と `frontend/coverage/` の HTML / lcov report を確認する。
バックエンドは `spring-boot-starter-test`, `spring-security-test`, `h2` を使い、`docker compose run --rm backend mvn test` で実行する。

Unit coverage は Maven の `coverage` profile で JaCoCo を有効にし、`backend/target/site/jacoco/` に report を出力する。
CI では JaCoCo CSV から domain / application 層の line coverage を集計し、80% 未満なら失敗させる。
長期目標は 100% とし、未カバーの分岐や例外系は段階的にテストを追加する。

## 6. ルール

- UT は外部サービスや実 DB に依存させない
- API contract、DB 方言、認証 filter chain の結合確認は [Integration Test](../integration/README.md) に寄せる
- 主要導線のブラウザ操作確認は [E2E Test](../e2e/README.md) に寄せる
- ケース一覧と実装済みテストは [Unit Test ケース](cases.md) に記録する
