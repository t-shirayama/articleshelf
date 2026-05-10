# Article ドメインモデル強化

## 状態

未対応

## 優先度

P2

## 目的

`Article` / `Tag` をデータ保持中心から、業務ルールと値の正規化を閉じ込めるドメインモデルへ寄せる。

## 対象

- `backend/src/main/java/com/articleshelf/domain/article/Article.java`
- `backend/src/main/java/com/articleshelf/domain/article/Tag.java`
- rating / tag name / article url / title / summary / notes
- article update / status change / metadata apply logic

## 対応内容

- `Rating`、`TagName`、`ArticleUrl` など導入しやすい値オブジェクトから検討する
- `changeStatus`、`updateContent`、`changeRating`、`replaceTags`、`applyMetadata` などの振る舞いを `Article` 側へ寄せる
- tag name の空白禁止、最大長、正規化、比較方針を domain に閉じ込める
- Service 側に散らばる入力正規化や業務ルールを必要な範囲で domain へ移す
- persistence / API DTO との変換境界を明確にする

## 完了条件

- rating や tag name などの主要ルールが domain 型または domain method に集約されている
- Article 更新時に Service が Article を丸ごと作り直す責務が減っている
- domain は Spring / JPA に依存しない状態を維持している
- domain unit tests と architecture docs が更新されている

## 根拠

Backend レビューで、`Article` と `Tag` がコンストラクタと getter 中心で、DDD を掲げるには貧血ドメインモデルに見えやすいと指摘されたため。
