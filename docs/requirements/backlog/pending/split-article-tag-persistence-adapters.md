# Article / Tag 永続化 Adapter 分割

## 状態

未対応

## 優先度

高

## 目的

`JpaArticleRepository` が Article と Tag の永続化責務を同時に持つ状態を解消し、ArticleRepository / TagRepository の adapter 境界を明確にする。

## 対象

- `backend/src/main/java/com/articleshelf/infrastructure/persistence/JpaArticleRepository.java`
- Article / Tag JPA entity
- Article / Tag Spring Data repository
- Article / Tag persistence mapper
- LIKE escape / search query helper

## 対応内容

- `JpaArticleRepositoryAdapter implements ArticleRepository` と `JpaTagRepositoryAdapter implements TagRepository` へ分割する
- Article / Tag / ArticleTag の entity、Spring Data repository、mapper の配置を整理する
- Tag rename、merge、unused delete などの tag 操作を tag adapter 側に寄せる
- Article search / save / find の責務を article adapter 側に寄せる
- 必要に応じて package を `persistence/article`、`persistence/tag`、`persistence/auth` などへ分ける

## 完了条件

- ArticleRepository と TagRepository の infrastructure 実装が分離されている
- Article / Tag mapper が責務ごとに分かれている
- 既存の article / tag API と persistence integration tests が維持されている
- PostgreSQL 実体を使う persistence IT で検索、タグ操作、制約が確認されている
- architecture / data docs が新しい責務分離に追従している

## 根拠

Backend レビューで、`JpaArticleRepository` が Article 保存 / 検索、Tag 保存 / rename / merge / delete、mapper、LIKE escape をまとめて背負っており、永続化レイヤーで最もリファクタ効果が大きいと指摘されたため。
