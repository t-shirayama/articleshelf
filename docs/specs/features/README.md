# 機能仕様

ArticleShelf の機能仕様の入口です。
画面別の挙動は [UI仕様](../ui/README.md)、見た目や配置は [デザイン文書](../../designs/README.md) を参照します。

## 詳細文書

- [記事機能](articles.md): 記事一覧、記事詳細、記事追加
- [カレンダー](calendar.md): 登録日 / 既読日の月表示
- [タグと検索](tags-and-search.md): タグ管理、検索・フィルター
- [UI仕様](../ui/README.md): 画面挙動、状態遷移、エラー表示、レスポンシブ
- [デザイン文書](../../designs/README.md): 見た目、配置、余白、コンポーネント設計

## 現在の補足

- アカウント設定ダイアログには、Chrome 拡張機能のローカル配布 zip ダウンロードリンクと導入手順を表示する
- Chrome 拡張機能の初期版は token を保持せず、現在ページの `url` / `title` を `GET /articles?source=extension&articleUrl=...&articleTitle=...` として認証済み Web アプリへ渡す
- 未認証時は `/login?returnTo=...` を経由し、ログイン後に拡張機能経由の追加導線へ戻る
