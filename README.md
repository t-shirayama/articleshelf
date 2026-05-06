# ReadStack

ReadStackは、URLからOGPを取得して読んだ記事をストックし、学習や仕事の資産として整理・管理するアプリです。

## コンセプト

- 記事URLを入力してOGPを取得し、読んだ技術記事をストック
- 記事のURL / タイトル / タグ / メモ / 読了日を一元管理
- 読了状況やお気に入り、タグで振り返りや検索を高速化
- デスクトップとモバイルの双方で使いやすいUI設計

## 主要機能

- 記事追加（URL、タイトル、タグ、メモ、読了日）
- 未読 / 読了ステータスの切り替え
- タグと検索による絞り込み
- 記事詳細ビューでメモを確認・編集
- 資産化した読書履歴の一覧表示

## 技術スタック

- フロントエンド: Vue.js + TypeScript + Vuetify
- バックエンド: Java / Spring Boot
- データベース: PostgreSQL
- 実行環境: Docker / Docker Compose
- 認証・API: RESTful API設計
- UI: Vuetify とカスタムCSSを組み合わせたレスポンシブ対応

## 画面イメージ

- ホーム / すべての記事一覧
- 記事詳細ビュー
- 追加モーダル / 追加フォーム
- タグフィルター
- モバイル表示対応

## 使い方

1. リポジトリをクローン
2. 開発用ブランチを作成
3. Vue.js + TypeScript フロントエンドと Spring Boot バックエンドを実装
4. PostgreSQL で記事データを永続化
5. バックエンドは Docker コンテナでビルド・起動できる構成を前提とする

## 開発環境

- 開発時は `docker compose up --build` でフロントエンド、バックエンド、PostgreSQL をまとめて起動する
- PostgreSQL も Docker Compose で起動し、バックエンドからコンテナ間通信で接続する
- フロントエンドは Vite のホットリロードに対応し、`frontend/src` などの変更がブラウザへ反映される
- バックエンドは Spring Boot DevTools と Maven compile 監視により、Java / resources の変更後に自動で再起動される
- Maven はローカルに直接インストールして使う前提ではなく、確認やビルドは Docker 上の `backend` コンテナ経由で実行する
- 例: テストは `docker compose exec backend mvn test`、パッケージ確認は `docker compose exec backend mvn -DskipTests package`
- API は `http://localhost:8080`、フロントエンドは `http://localhost:5173` で起動する

## CI

- GitHub Actions で push / pull request ごとに CI を実行する
- フロントエンドは `npm run build` で TypeScript の型チェックと Vite ビルドを行い、構文崩れやビルド不能を検知する
- バックエンドは Java 21 で `mvn -DskipTests package` を実行し、コンパイルエラーや設定崩れを検知する

## 現状整理

- プロジェクトの進捗と残作業は [docs/project-status.md](docs/project-status.md) に整理しています

## 今後の拡張案

- OCRや画像解析による自動記事抽出
- ブラウザ拡張やクリップボード入力の支援
- OGP自動取得やAI要約機能
- ユーザー認証 / アカウント同期
- 学習ログとの連携

## 目的

ReadStackは、単なる“あとで読む”リストではなく、読んだ記事を振り返りやすい「学習資産」に変えるアプリです。記事とメモを一体化して、技術情報の蓄積と再利用を支援します。
