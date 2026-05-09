# ArticleShelf Specification

## 概要

ArticleShelf の現在の実装仕様をまとめる入口文書です。
詳細は肥大化を防ぐため、`docs/specification/` 配下の責務別フォルダに分割します。

## 目次

- [機能仕様](features/README.md): 記事、カレンダー、タグ、検索・フィルター
- [API仕様](api/README.md): API 共通方針、記事 API、タグ API、エラーレスポンス
- [認証仕様](auth/README.md): 認証方式、トークン、アカウント API、フロントエンド認証状態
- [データモデル](data/README.md): User、RefreshToken、Article、Tag、ArticleTag
- [UI仕様](ui/README.md): 画面挙動、レスポンシブ、表示言語、エラー表示
- [セキュリティ仕様](security/README.md): 認証・認可、CSRF/CORS、secret、rate limit、SSRF、Markdown sanitization
- [品質仕様](quality/README.md): 入力検証、信頼性、性能、保守性、テストと CI
