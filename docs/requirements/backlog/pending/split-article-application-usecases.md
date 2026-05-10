# Article アプリケーションユースケース分割

## 状態

未対応

## 優先度

高

## 目的

`ArticleService` に集中している記事追加、更新、削除、詳細取得、検索、タグ解決、metadata 反映の責務をユースケース単位へ分割し、変更理由を明確にする。

## 対象

- `backend/src/main/java/com/articleshelf/application/article/ArticleService.java`
- article command / query / response
- `ArticleMetadataProvider`
- article / tag resolving logic
- article controller から application 層への呼び出し

## 対応内容

- `AddArticleUseCase`、`UpdateArticleUseCase`、`DeleteArticleUseCase`、`FindArticleQuery`、`SearchArticlesQuery` などへ責務分割する
- `ArticleMetadataEnrichmentService` や `ArticleTagResolver` など、外部 metadata とタグ解決の補助責務を切り出す
- command / query / mapper の配置を整理する
- 既存 API のリクエスト / レスポンス契約を維持する
- Clean Architecture dependency test の意図に沿う依存方向を維持する

## 完了条件

- `ArticleService` の肥大化が解消され、主要操作がユースケースまたは query 単位に分かれている
- article controller は分割後の application API を呼び出している
- 既存の記事追加、更新、削除、検索、詳細取得の挙動が維持されている
- 関連する backend tests と architecture docs が更新されている

## 根拠

Backend レビューで、`ArticleService` が検索条件正規化、重複 URL チェック、OGP 取得、metadata 反映、Article 生成、Tag 解決、更新可否判断、Response 変換をまとめて持っており、アプリケーション層の責務が詰まりすぎていると指摘されたため。
