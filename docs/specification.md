# ReadStack Specification

## 1. 機能仕様

### 1.1 記事一覧

- すべての記事を一覧表示
- タイトル、ドメイン、タグ、読了ステータス、読了日を確認
- 検索バーでタイトル・URL・メモを横断検索
- タグで絞り込み
- 未読 / 読了 / お気に入りなどでフィルタリング

### 1.2 記事詳細

- 記事のURL、タイトル、概要、タグ、メモ、読了日を表示
- メモを入力・編集できる
- 読了ステータスを切り替えられる
- タグを追加・削除できる
- 読了日を更新できる

### 1.3 記事追加

- URL、タイトル、タグ、メモ、読了日を入力して追加
- URLからOGPを取得してタイトル・概要・サムネイルを自動補完
- 既存記事の重複チェック
- 画像添付は将来的な拡張として検討

### 1.4 タグ管理

- タグを複数選択できる
- 記事にタグを付与可能
- よく使うタグを優先表示

### 1.5 検索・フィルター

- テキスト検索: タイトル、URL、メモ
- ステータスフィルタ: 未読 / 読了 / すべて
- タグフィルタ: 複数タグ選択

## 2. データモデル

### 2.1 Article

- id: UUID
- url: string
- title: string
- summary: string (任意)
- status: enum(`UNREAD`, `READ`)
- readDate: date (任意)
- favorite: boolean
- notes: text
- createdAt: timestamp
- updatedAt: timestamp

### 2.2 Tag

- id: UUID
- name: string
- createdAt: timestamp
- updatedAt: timestamp

### 2.3 ArticleTag

- articleId: UUID
- tagId: UUID

## 3. API仕様

### エンドポイント

- `GET /api/articles`
  - 説明: 記事一覧を取得
  - パラメータ: `status`, `tag`, `search`

- `GET /api/articles/{id}`
  - 説明: 記事詳細を取得

- `POST /api/articles`
  - 説明: 記事を追加
  - リクエスト: `url`, `title`, `summary`, `status`, `readDate`, `favorite`, `notes`, `tags`

- `PUT /api/articles/{id}`
  - 説明: 記事を更新

- `DELETE /api/articles/{id}`
  - 説明: 記事を削除

- `GET /api/tags`
  - 説明: タグ一覧を取得

- `POST /api/tags`
  - 説明: タグを追加

## 4. UI仕様

### 4.1 共通

- レスポンシブデザイン
- 明確なステータス表示
- 情報はカード・テーブル形式で整理
- 一覧画面に詳細パネルを同時表示せず、記事選択時は詳細画面へ遷移

### 4.2 記事一覧画面

- 検索バーとフィルタを画面上部に配置
- 記事カード/リストにタイトル、URL、タグ、ステータスを表示
- クリックで記事詳細へ遷移
- 追加ボタンを常時表示

### 4.3 記事詳細画面

- 記事ヘッダーにタイトル、URL、タグ、ステータスを表示
- メイン領域に編集フォーム、右側にメタ情報（読了日、favorite）と操作ボタンを表示
- メモ欄は編集可能

### 4.4 追加モーダル

- URL入力欄
- タイトル入力欄
- タグ選択/追加フォーム
- 読了日入力/日付ピッカー
- メモ入力欄
- 保存 / キャンセル

## 5. 非機能仕様

- フロントエンド: Vue.js 3 + Vue Router + Pinia（状態管理）
- バックエンド: Spring Boot 3 + Spring Data JPA
- DB: PostgreSQL
- バックエンドとDBは Docker / Docker Compose で起動できる構成とする
- バリデーション: フロントエンドとバックエンド両方で実施
- API形式: JSON
- ロギング: バックエンドでリクエスト/エラーを記録
- テスト: 単体テスト / 結合テスト
