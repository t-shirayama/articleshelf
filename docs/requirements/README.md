# ArticleShelf Requirements

## 概要

ArticleShelfは、読んだ技術記事を「資産」として蓄積・管理するストックアプリです。記事のURLやタイトル、タグ、メモ、既読日を保存し、振り返りや学習のために活用できるようにします。

このファイルは要件ドキュメントの入口として保守し、詳細は `docs/requirements/` 配下の項目別ファイルに記載します。
今後やるべきこと、残作業、構想段階のアイデアは [Backlog](backlog.md) に集約します。

## 目次

- [プロダクト要件](product.md): ターゲットユーザー、主な課題、解決すること
- 機能要件
  - [記事追加](functional/article-create.md): URL入力、OGP補完、重複URL、追加フォーム
  - [記事状態](functional/article-status.md): 未読 / 既読、お気に入り、おすすめ度、並び替え
  - [記事詳細](functional/article-detail.md): 詳細確認、編集、削除、詳細内タグ操作
  - [カレンダー](functional/calendar.md): 追加日 / 既読日の月次確認
  - [タグ](functional/tags.md): タグ追加 / 選択 / 名称変更 / 統合 / 未使用削除
  - [検索・フィルタ](functional/search-filter.md): 検索、タグフィルタ、状態 / 日付 / おすすめ度フィルタ
- 非機能要件
  - [技術スタック](non-functional/technology.md): Vue.js、TypeScript、Spring Boot、PostgreSQL
  - [セキュリティ](non-functional/security.md): 認証、ユーザースコープ、SSRF、Markdown安全化
  - [画面・運用](non-functional/ui-and-operation.md): レスポンシブ対応、API分離、拡張性、永続化
- [Backlog](backlog.md): 今後のタスク、残作業、構想段階のアイデア
