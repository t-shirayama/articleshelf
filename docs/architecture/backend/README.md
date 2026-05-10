# Backend Architecture

バックエンドは DDD / クリーンアーキテクチャの考え方で実装します。
依存関係は内側から外側へ流れるように設計し、ドメインモデルが中心になります。

## レイヤー構成

- `domain`: ドメインモデル、集約、値オブジェクト、ドメインサービス、リポジトリインターフェース
- `application`: ユースケース、アプリケーションサービス、DTO、外部機能を抽象化するポート（インターフェース）
- `infrastructure`: 永続化実装、外部APIクライアント、OGP取得、リポジトリ実装
- `adapter`: Web/API レイヤー、コントローラー、HTTP 入力/出力のマッピング
- `config`: Spring の設定、CORS、Bean 定義
- `infrastructure.security`: Spring Security、Spring Security JOSE による JWT 発行 / 検証、refresh token hash、password encoder

## DDDの依存関係ルール

- `domain` はどのレイヤーにも依存しない
- `application` は `domain` に依存する
- `infrastructure` は `application` と `domain` のインターフェースに依存する
- `adapter` は `application` のユースケースに依存し、`domain` を直接扱わない
- `dto` は API 入出力とアプリケーション境界のために使う
- `backend/src/test/java/com/articleshelf/architecture/CleanArchitectureDependencyTest.java` で、`domain` から外側の層や Spring/Jakarta への依存、`application` から `adapter` / `infrastructure` / `config` への依存、`adapter` から `infrastructure` / `config` への依存、`infrastructure` から `adapter` への依存を検査する
- GitHub Actions の `backend-check` job は `docker compose run --rm backend mvn test -Dtest=CleanArchitectureDependencyTest` を実行するため、この依存関係チェックも CI で必ず実行される

## Backend 品質ゲート

backend の CI は、構造の崩れ、静的解析の指摘、ドメイン / アプリケーション層のテスト不足、PostgreSQL 方言差、主要導線の破壊を段階的に検知する。

- `backend-check`: Docker 経由の Maven で compile、SpotBugs、Clean Architecture dependency test を実行する
- `backend-unit`: domain / application を中心に UT を coverage 付きで実行し、JaCoCo CSV から domain / application line coverage 80% 以上を要求する
- `backend-integration`: Spring Boot と PostgreSQL 実体を使い、認証境界、Repository 検索、DB 制約、JPA validate を確認する
- `e2e`: backend / frontend / DB を Compose で起動し、記事追加や認証を含む P0 導線を Playwright Chromium で確認する

CI の段階構成とコマンドは [CI / CD Architecture](../ci-cd/README.md)、テストの役割分担は [テスト戦略](../../testing/README.md) を正本とする。

## パッケージ構成

- `com.articleshelf.domain.article`
  - `Article` (集約ルート)
  - `Tag` (値オブジェクト／エンティティ)
  - `ArticleRepository` (インターフェース)
  - `TagRepository` (タグ管理用インターフェース)
- `com.articleshelf.application.article`
  - `ArticleService` / `ArticleUseCase`
  - `TagService` (タグ一覧、作成、名称変更、マージ、未使用タグ削除)
  - `AddArticleCommand`
  - `ArticleResponse`
  - `ArticleMetadataProvider` (OGP 取得など外部メタデータ取得のポート)
- `com.articleshelf.application.auth`
  - `AuthService`
  - `AuthUserRepository` / `RefreshTokenRepository` (認証永続化ポート)
  - `AccessTokenIssuer` / `RefreshTokenSecretService` / `PasswordHasher` / `IdGenerator` (セキュリティ実装と実行環境依存値のポート)
- `com.articleshelf.infrastructure.persistence`
  - `ArticleEntity`
  - `JpaArticleRepository`
  - `JpaAuthUserRepository`
  - `JpaRefreshTokenRepository`
- `com.articleshelf.infrastructure.ogp`
  - `OgpClient`
  - `OgpService` (ArticleMetadataProvider の実装)
- `com.articleshelf.adapter.web`
  - `ArticleController`
  - `TagController`
  - `AuthController` / `UserController` は request DTO と use case 呼び出しを担当し、session cookie 発行 / 削除は `SessionCookieWriter`、CSRF 検証は `CsrfTokenValidator` に委譲する
  - `ArticleRequestMapper` は article request DTO から application command への変換を担当する
  - `ClientRequestContext` は User-Agent と client IP の取得を集約する

## ドメイン中心の実装方針

- `Article` と `Tag` をドメインオブジェクトとして扱う
- URL/OGP取得ロジックはインフラ層で実装し、アプリケーション層は `ArticleMetadataProvider` ポート経由で呼び出す
- OGP 取得時の SSRF 対策、redirect、body size、Content-Type 制限などの具体仕様は [セキュリティ仕様](../../specs/security/README.md) を正本とする
- 記事ユースケースは `ArticleService`、タグ管理ユースケースは `TagService` に分け、コントローラーも各サービスへ直接委譲する
- 永続化と記事一覧の基本検索条件（ステータス、単一タグ、検索語、お気に入り）は `ArticleRepository` 経由で扱い、タグ一覧・名称変更・マージ・未使用削除は `TagRepository` 経由で扱う
- 認証ユースケースは `AuthService` に閉じ、JPA Entity、Spring Data Repository、JWT発行、refresh token hash、password encoder は application 層のポート越しに扱う。JWT の署名・検証・`exp` 検証・`alg` 扱いは自前実装せず、`spring-security-oauth2-jose` の encoder / decoder に委譲する
- 認証ユースケースの時刻、UUID、CSRF token 用 random source は `Clock`、`IdGenerator`、`SecureRandom` Bean として注入し、token rotation や invalidation のテストで固定値を使えるようにする
- API層は DTO を受け取り、アプリケーションサービスに変換して処理する
- API層の session cookie / CSRF の HTTP 詳細は dedicated adapter component に集約し、Controller に重複実装しない
- request DTO から application command への変換や client context 取得は adapter 内の helper / mapper に寄せ、Controller は HTTP 入出力と use case 呼び出しに集中させる

## 実装上の分離ポイント

- URLからOGP取得はバックエンドで実装済みで、外部取得は `ArticleMetadataProvider` ポート越しに扱う
- ユーザー認証を追加し、記事 / タグ API は user scoped repository で分離済み。`article_tags.user_id` と複合 FK も導入し、DB レベルでも article / tag の user mismatch を拒否する
