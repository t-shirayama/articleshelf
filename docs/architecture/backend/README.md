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
  - `AccessTokenIssuer` / `RefreshTokenSecretService` / `PasswordHasher` (セキュリティ実装のポート)
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

## ドメイン中心の実装方針

- `Article` と `Tag` をドメインオブジェクトとして扱う
- URL/OGP取得ロジックはインフラ層で実装し、アプリケーション層は `ArticleMetadataProvider` ポート経由で呼び出す
- OGP 取得のインフラ実装は、リクエスト前と redirect ごとに URL / DNS 解決後 IP を検証し、localhost、private IP、link-local、multicast、metadata endpoint へのアクセスを拒否する。HTTP body は上限を設け、HTML 以外は解析しない
- 記事ユースケースは `ArticleService`、タグ管理ユースケースは `TagService` に分け、コントローラーも各サービスへ直接委譲する
- 永続化と記事一覧の基本検索条件（ステータス、単一タグ、検索語、お気に入り）は `ArticleRepository` 経由で扱い、タグ一覧・名称変更・マージ・未使用削除は `TagRepository` 経由で扱う
- 認証ユースケースは `AuthService` に閉じ、JPA Entity、Spring Data Repository、JWT発行、refresh token hash、password encoder は application 層のポート越しに扱う。JWT の署名・検証・`exp` 検証・`alg` 扱いは自前実装せず、`spring-security-oauth2-jose` の encoder / decoder に委譲する
- API層は DTO を受け取り、アプリケーションサービスに変換して処理する

## 実装上の分離ポイント

- URLからOGP取得はバックエンドで実装済みで、外部取得は `ArticleMetadataProvider` ポート越しに扱う
- ユーザー認証を追加し、記事 / タグ API は user scoped repository で分離済み。`article_tags.user_id` と複合 FK も導入し、DB レベルでも article / tag の user mismatch を拒否する
