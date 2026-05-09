# ArticleShelf Test Strategy

最終更新: 2026-05-09

ArticleShelf のテストは、記事を「読んだ知識資産」として安全に登録・検索・更新できることを継続的に保証するために整備する。
この README はテスト文書の入口とし、Unit / Integration / E2E の詳細は配下ディレクトリに分割する。

## 1. 目的

経営・プロダクト観点では、リリース前後に次の品質リスクを下げることを目的にする。

- ユーザーが保存した記事、メモ、タグ、既読履歴を失わない
- 重要な導線である記事追加、一覧検索、詳細編集、削除、カレンダー確認が壊れていない
- 認証追加後に、他ユーザーのデータが見えない
- UI 改修やレスポンシブ対応後も、主要な操作が継続して使える
- CI により、公開ブランチへ壊れた変更が入る可能性を下げる

## 2. テスト方針

ArticleShelf では、実行速度と保守性を重視し、次の比率を目安に整備する。

| 種別 | 主目的 | 件数の目安 | 実行タイミング |
| --- | --- | --- | --- |
| UT | 関数、ドメイン、ユースケース単位の仕様を高速に検証 | 多め | ローカル、PR、push |
| IT | API、DB、Spring 設定、永続化、認証境界を検証 | 中程度 | PR、main push |
| E2E | ブラウザ上の主要ユーザーシナリオを検証 | 少数精鋭 | PR、main push、リリース前 |

E2E は便利だが壊れやすく遅くなりやすい。細かい分岐は UT / IT へ寄せ、E2E は役員説明やリリース判定で「主要導線が動く」と説明できる代表シナリオに絞る。

## 3. 優先度

| 優先度 | 意味 | 対象例 |
| --- | --- | --- |
| P0 | リリース判定に必須。壊れるとデータ消失、情報漏えい、主要導線停止につながる | 記事追加、保存、編集、削除、認証、ユーザースコープ、CI |
| P1 | MVP の信頼性に必要。壊れると体験品質が大きく落ちる | 検索、フィルタ、タグ、カレンダー、Markdown 表示 |
| P2 | 使い勝手や追加機能の品質を上げる | 細かな表示状態、境界値、アクセシビリティ補助 |

## 4. 現在の前提

- 採用技術と推奨バージョンは [技術スタック](../architecture/technology.md) に従う
- フロントエンド確認: `npm run build`
- フロントエンド UT: `npm run test:unit`
- フロントエンド UT coverage: `npm run test:unit:coverage`
- ブラウザ E2E: `npm run test:e2e`
- バックエンド確認: ローカル `mvn` ではなく Docker 経由で `docker compose run --rm backend mvn test` を実行する
- バックエンド UT coverage: `docker compose run --rm backend mvn -Pcoverage test -Dtest='ArticleTest,PasswordPolicyTest,UsernamePolicyTest,ArticleServiceTest,AuthRateLimiterTest,ApiExceptionHandlerTest,JwtTokenServiceTest,OgpRequestGuardTest,ProductionEnvironmentValidatorTest,AuthAndArticleIntegrationTest'`
- 既存 CI: `.github/workflows/ci.yml` でフロントエンド UT / build、バックエンド UT / IT、E2E を実行する

## 5. 詳細文書

- [Unit Test](unit/README.md): UT の目的、スコープ、実装方針、実行方法、ルール
- [Unit Test ケース](unit/cases.md): UT ケース一覧と実装済み UT
- [Integration Test](integration/README.md): IT の目的、スコープ、実装方針、実行方法、DB / API / 認証境界の確認ルール
- [Integration Test ケース](integration/cases.md): IT ケース一覧と実装済み IT
- [E2E Test](e2e/README.md): E2E の目的、スコープ、Playwright 実行方針、操作フローのルール
- [E2E Test ケース](e2e/cases.md): E2E シナリオ一覧と実装済み E2E
- [テストデータ](test-data.md): 通常起動、IT、E2E、初期 ADMIN のテストデータ方針
- [CI](ci.md): CI 連携、workflow 構成、完了条件
