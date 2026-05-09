# Integration Test

## 1. 目的

IT は、複数レイヤーを結合したときの契約を検証する。
ArticleShelf では API、DB、JPA mapping、Spring 設定、CORS、認証、トランザクション、エラーレスポンスを中心に確認する。

## 2. スコープ

- `@SpringBootTest` または `@WebMvcTest` による API 検証
- PostgreSQL と JPA の結合
- リポジトリの検索、保存、削除
- API の request / response JSON
- バリデーションと例外ハンドリング
- CORS 設定
- 認証追加後の security filter chain
- ユーザーごとのデータ分離
- `/actuator/health` など公開時の health check

## 3. 対象外

- 実外部サイトへの OGP 取得
- 実ブラウザの UI 操作
- Render / GitHub Actions など外部環境そのものの稼働保証
- 画面デザインの見た目

## 4. 実装方針

| 領域 | 方針 |
| --- | --- |
| API | Spring Boot Test, MockMvc |
| DB | Testcontainers PostgreSQL または Docker Compose 上の PostgreSQL |
| 認証 | Spring Security Test |
| OGP | WireMock などの stub server、または `ArticleMetadataProvider` の test double |

リリース前後の DB 互換性確認では、本番に近い PostgreSQL で検証できるよう Testcontainers の採用を推奨する。
ただし Docker in Docker の CI 負荷が高い場合は、まず `docker compose run --rm backend mvn test` のように backend コンテナからテストを実行する方式に寄せる。
現行 IT は H2 の PostgreSQL mode を使い、Spring Security / MockMvc / JPA / API 契約を軽量に検証する。

## 5. 動作確認方法

- 通常の backend test: `docker compose run --rm backend mvn test`
- 対象 test class のみ確認: `docker compose run --rm backend mvn -Dtest=<TestClassName> test`
- PostgreSQL 実体での persistence IT が必要な場合は、対象条件を `JpaArticleRepositoryPostgresIntegrationTest` などに追加して Docker 経由で実行する

## 6. テストデータ / test double

- IT は既存データに依存せず、ケースごとに必要なデータを作成する
- OGP 取得は application port の test double、または stub server に差し替える
- API 境界では request / response JSON、validation、例外変換、認証境界を確認する
- DB 方言差が重要なケースでは PostgreSQL 実体を使う

## 7. CI での扱い

- `backend-integration` / `frontend-integration` で backend の Spring Boot / PostgreSQL IT と frontend の `*.integration.test.ts` を分けて実行する
- DB 方言差や migration 影響がある変更では、PostgreSQL 実体を使う確認を優先する

## 8. ルール

- Maven を使った確認はローカル `mvn` ではなく Docker 経由で実行する
- OGP の外部通信は実外部サイトへ依存させず、test double や stub server を使う
- Spring Data JPA の `@Query`、JPQL、native SQL、Repository の検索条件、Flyway migration、DB 制約を変更した場合は、H2 だけでなく PostgreSQL 実体を使う persistence IT を実行する
- 特に `LIKE`、`concat`、`coalesce`、nullable parameter、enum、UUID、日付、JOIN を含む条件は DB 方言や型推論の差が出やすいため、PostgreSQL 実体で確認する
- ケース一覧と実装済みテストは [Integration Test ケース](cases.md) に記録する
