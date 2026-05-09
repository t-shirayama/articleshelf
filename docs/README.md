# ArticleShelf Docs

ArticleShelf の設計、要件、仕様、テスト、運用情報を整理する入口です。
`docs/` 直下にはこの README だけを置き、詳細文書は目的ごとのディレクトリに格納します。

## Docs 構成ルール

- `docs/<area>/` 直下は、原則として `README.md` と責務別フォルダだけを置く
- 例外として、画像、スクリーンショット、生成キャプチャなどの資産専用フォルダは `README.md` を必須にしない
- `docs/<area>/<responsibility>/` 配下は、小さな責務なら `README.md` のみで完結させる
- 複数の読者、更新理由、正本が混ざる場合や、API、UI、テストケース、運用手順のように更新頻度が異なる場合は、`README.md` を索引にして詳細 `.md` を分割する
- 今後のタスク、残作業、構想段階のアイデア、技術的負債は [Backlog](requirements/backlog/README.md) を唯一の保存先にする

## 目次

- [プロダクト](product/README.md): プロダクトビジョン、用語集
- [要件](requirements/README.md): 機能要件、非機能要件、Backlog
- [仕様](specs/README.md): 機能仕様、API、認証、データモデル、UI、セキュリティ、品質仕様
- [アーキテクチャ](architecture/README.md): 全体構成、技術スタック、frontend / backend 詳細、DB、API フロー、実行環境、CI / CD
- [デザイン](designs/README.md): UI 方針、コンポーネント設計、レスポンシブ仕様、スマホ対応、現行 UI スクリーンショット
- [テスト](testing/README.md): UT / IT / E2E の方針、ケース、CI 連携
- [デプロイ構成](deployment/README.md): 無料枠を中心にした公開構成と CI
