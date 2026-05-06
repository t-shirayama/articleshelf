# ReadStack Requirements

## 概要

ReadStackは、読んだ技術記事を「資産」として蓄積・管理するストックアプリです。記事のURLやタイトル、タグ、メモ、読了日を保存し、振り返りや学習のために活用できるようにします。

このファイルは要件ドキュメントの入口として保守し、詳細は `docs/requirements/` 配下の項目別ファイルに記載します。
今後追加したい機能や構想段階のアイデアは、まず `docs/requirements/future-considerations.md` に整理します。

## 目次

- [プロダクト要件](requirements/product.md): ターゲットユーザー、主な課題、解決すること
- 機能要件
  - [記事管理](requirements/functional/article-management.md): 記事追加、詳細確認、編集、削除、未読 / 読了管理
  - [タグ・検索](requirements/functional/tag-search.md): タグ追加 / 選択、検索、タグフィルタ
- 非機能要件
  - [技術スタック](requirements/non-functional/technology.md): Vue.js、TypeScript、Spring Boot、PostgreSQL
  - [画面・運用](requirements/non-functional/ui-and-operation.md): レスポンシブ対応、API分離、拡張性、永続化
- [追加検討項目](requirements/future-considerations.md): 一覧上の読了切り替え、タグUI改善、カレンダー、Markdownメモ、Chrome拡張、画像・スクリーンショット、AI要約、認証・同期
