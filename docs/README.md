# ArticleShelf Docs

ArticleShelf の設計、要件、仕様、テスト、運用情報を整理する入口です。
`docs/` 直下にはこの README だけを置き、詳細文書は目的ごとのディレクトリに格納します。

この README は人間がドキュメント全体を読み始めるための入口です。`AGENTS.md` は Codex / AI エージェントや作業者が変更時の判断基準として使う運用ルールです。

## 最初に読む順

1. [プロジェクト README](../README.md): アプリ概要、試し方、主要機能、開発コマンド
2. [Docs README](README.md): ドキュメント構成、参照順、主要文書への入口
3. [技術スタック](architecture/technology/README.md): 採用技術、推奨バージョン、開発環境、テストツール
4. [仕様](specs/README.md): 機能仕様、API、認証、データモデル、UI、セキュリティ、品質仕様
5. [テスト](testing/README.md): UT / IT / E2E の方針、ケース、CI 連携
6. [Backlog](requirements/backlog/README.md): 今後のタスク、残作業、構想段階のアイデア、技術的負債

## Docs 構成ルール

- `docs/<area>/` 直下は、原則として `README.md` と責務別フォルダだけを置く
- 例外として、画像、スクリーンショット、生成キャプチャなどの資産専用フォルダは `README.md` を必須にしない
- `docs/<area>/<responsibility>/` 配下は、小さな責務なら `README.md` のみで完結させる
- 複数の読者、更新理由、正本が混ざる場合や、API、UI、テストケース、運用手順のように更新頻度が異なる場合は、`README.md` を索引にして詳細 `.md` を分割する
- 今後のタスク、残作業、構想段階のアイデア、TODO、技術的負債は [Backlog](requirements/backlog/README.md) を唯一の保存先にし、1ファイル1タスクで状態別フォルダに置く

## 目次

- [プロダクト](product/README.md): プロダクトビジョン、用語集
- [要件](requirements/README.md): 機能要件、非機能要件、Backlog
- [仕様](specs/README.md): 機能仕様、API、認証、データモデル、UI、セキュリティ、品質仕様
- [アーキテクチャ](architecture/README.md): 全体構成、技術スタック、frontend / backend 詳細、DB、API フロー、実行環境、CI / CD
- [技術スタック](architecture/technology/README.md): 採用技術、推奨バージョン、開発環境、テストツール
- [実行環境](architecture/runtime/README.md): Docker Compose、開発環境、コンテナ方針
- [CI / CD](architecture/ci-cd/README.md): GitHub Actions の段階構成、品質ゲート、デプロイとの関係
- [デザイン](designs/README.md): UI 方針、コンポーネント設計、レスポンシブ仕様、スマホ対応、現行 UI スクリーンショット
- [テスト](testing/README.md): UT / IT / E2E の方針、ケース、CI 連携
- [デプロイ構成](deployment/README.md): 無料枠を中心にした公開構成と CI
