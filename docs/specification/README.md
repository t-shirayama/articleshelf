# ReadStack Specification

## 概要

ReadStackの機能仕様、データモデル、API仕様、UI仕様、非機能仕様をまとめる入口文書です。詳細は肥大化を防ぐため、`docs/specification/` 配下の項目別ファイルに分割します。

## 目次

- [機能仕様](features.md): 記事一覧、記事詳細、カレンダー、記事追加、タグ管理、検索・フィルター
- [データモデル](data-model.md): Article、Tag、ArticleTag
- [API仕様](api.md): 記事API、タグAPI
- [ユーザー登録・ログイン・JWT認証設計](authentication.md): 認証方式、トークン、ユーザースコープ、API、移行方針
- [UI仕様](ui.md): 共通、記事一覧画面、記事詳細画面、追加モーダル
- [非機能仕様](non-functional.md): フロントエンド、バックエンド、DB、バリデーション、ロギング、テスト
