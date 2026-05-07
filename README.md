# ReadStack

ReadStackは、URLからOGPを取得して読んだ記事をストックし、学習や仕事の資産として整理・管理するアプリです。

## コンセプト

- 記事URLを入力してOGPを取得し、読んだ技術記事をストック
- 記事のURL / タイトル / タグ / メモ / 既読日を一元管理
- 既読状況やお気に入り、タグで振り返りや検索を高速化
- デスクトップとモバイルの双方で使いやすいUI設計

## 主要機能

- 記事追加（URL、タイトル、タグ、メモ、既読日、おすすめ度、あとで読む）
- URL からの OGP 取得によるタイトル / 概要 / サムネイル補完
- 未読 / 既読ステータス、お気に入り、おすすめ度の切り替え
- タグ、検索、おすすめ度、登録日範囲、既読日範囲による絞り込み
- 登録日 / 更新日 / 既読日 / タイトル / おすすめ度での並び替え
- 記事詳細ビューで概要、タグ、メモ、ステータス、既読日、おすすめ度を確認・編集
- 月ごとの追加日 / 既読日を確認できるカレンダー表示
- OGPサムネイル画像のブラウザ内 IndexedDB キャッシュ
- 学習継続を促すサイドバー下部の画像付きメッセージ

## 技術スタック

- フロントエンド: Vue.js + TypeScript + Vuetify
- バックエンド: Java / Spring Boot
- データベース: PostgreSQL
- 実行環境: Docker / Docker Compose
- API: REST API
- UI: Vuetify とカスタムCSSを組み合わせたレスポンシブ対応

## 画面イメージ

- ホーム / すべての記事一覧
- 記事詳細ビュー
- 追加モーダル / 追加フォーム
- フィルタモーダル
- カレンダー表示
- モバイル表示対応

## 使い方

1. `docker compose up --build` で起動する
2. `http://localhost:5173` を開く
3. 「記事を追加」から URL を入力して保存する
4. 一覧、フィルタ、カレンダー、詳細編集で記事を整理する

## 開発環境

- 開発時は `docker compose up --build` でフロントエンド、バックエンド、PostgreSQL をまとめて起動する
- PostgreSQL も Docker Compose で起動し、バックエンドからコンテナ間通信で接続する
- フロントエンドは Vite のホットリロードに対応し、`frontend/src` などの変更がブラウザへ反映される
- フロントエンドは `node_modules` ボリュームが空の初回起動時でも、コンテナ起動時に依存を補完してから開発サーバーを立ち上げる
- バックエンドは Spring Boot DevTools と Maven compile 監視により、Java / resources の変更後に自動で再起動される
- Maven はローカルに直接インストールして使う前提ではなく、確認やビルドは Docker 上の `backend` コンテナ経由で実行する
- 例: テストは `docker compose exec backend mvn test`、パッケージ確認は `docker compose exec backend mvn -DskipTests package`
- API は `http://localhost:8080`、フロントエンドは `http://localhost:5173` で起動する

## 開発補助

- Codex 用のプロジェクト skill は `.codex/skills/` に配置している
- UI 調整では `.codex/skills/readstack-ui-polish/SKILL.md` と `docs/design.md` を参照する
- 実装とドキュメントの同期確認では `.codex/skills/readstack-change-sync/SKILL.md` を参照する
- `docs/designs/` の現行スクショ更新では `.codex/skills/readstack-design-capture/SKILL.md` を参照する
- Git hooks は `.githooks/` に配置している
- 初回だけ `git config core.hooksPath .githooks` を実行すると、コミット前にフロントエンド型チェックと軽い運用ルール確認が走る
- フロントエンド単体の型チェックは `cd frontend && npm run typecheck` で実行できる
- デザイン画像の再取得は `cd frontend && npm run capture:designs` で実行できる
- ブラウザ挙動の手動検証には `frontend` の `@playwright/test` を利用できる

## CI

- GitHub Actions で push / pull request ごとに CI を実行する
- フロントエンドは `npm run build` で TypeScript の型チェックと Vite ビルドを行い、構文崩れやビルド不能を検知する
- バックエンドは Java 21 で `mvn -DskipTests package` を実行し、コンパイルエラーや設定崩れを検知する
- Dependabot は `.github/dependabot.yml` で npm、Maven、GitHub Actions、Docker、Docker Compose の依存関係を週次確認する

## 現状整理

- プロジェクトの進捗と残作業は [docs/project-status.md](docs/project-status.md) に整理しています

## 今後の拡張案

- OCRや画像解析による自動記事抽出
- ブラウザ拡張やクリップボード入力の支援
- OGPサムネイルの手動再取得や画像保存先の拡張
- AI要約機能
- ユーザー認証 / アカウント同期
- 学習ログとの連携

## 目的

ReadStackは、単なる“あとで読む”リストではなく、読んだ記事を振り返りやすい「学習資産」に変えるアプリです。記事とメモを一体化して、技術情報の蓄積と再利用を支援します。
