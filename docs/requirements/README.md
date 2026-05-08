# ArticleShelf Requirements

## 概要

ArticleShelfは、読んだ技術記事を「資産」として蓄積・管理するストックアプリです。記事のURLやタイトル、タグ、メモ、既読日を保存し、振り返りや学習のために活用できるようにします。

このファイルは要件ドキュメントの入口として保守し、詳細は `docs/requirements/` 配下の項目別ファイルに記載します。
今後追加したい機能や構想段階のアイデアは、まず `docs/requirements/future-considerations.md` に整理します。

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
  - [画面・運用](non-functional/ui-and-operation.md): レスポンシブ対応、API分離、拡張性、永続化
- [追加検討項目](future-considerations.md): Chrome拡張、画像・スクリーンショット、OGP再取得、AI要約、アカウント管理、同期
