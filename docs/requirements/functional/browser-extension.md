# Chrome 拡張機能

## 目的

Chrome 拡張機能は、利用者がブラウザで読んでいる記事ページを、popup から ArticleShelf へ直接保存できるようにする。
初回だけ ArticleShelf の Web 認証を経由し、その後は拡張機能専用の保存用 token で記事の確認、追加、既読/未読更新を行う。

## 対象ユーザー

- Chrome を使って技術記事を読んでいる利用者
- ArticleShelf にログイン済み、または拡張機能の初回ログインで保存用 token を取得できる利用者
- GitHub Releases またはローカル開発環境から配布 zip を取得し、Chrome の Developer mode で拡張機能を読み込める利用者

## 利用者導線

- 利用者は記事ページを開いた状態で拡張機能 popup を開く
- 未ログインの場合は popup から ArticleShelf へログインし、拡張機能専用 token を取得する
- ログイン済みの場合は popup 内で現在ページを未読または既読として登録できる
- 登録済みの記事では現在状態が分かり、popup から既読または未読へ更新できる
- 保存中または更新中は操作ボタンを非活性にし、処理中であることを表示する

## 対応範囲

- 現在タブの `http` / `https` URL を ArticleShelf へ直接保存できる
- 現在タブのタイトルが取得できる場合は、記事タイトルとして保存リクエストに含める
- ArticleShelf の接続先 URL は、本番用とローカル確認用の配布物ごとに固定する
- 拡張機能 API でも通常の記事追加と同じ URL 検証、OGP 補完、重複 URL 確認、ユーザースコープを使う
- Help から配布 zip のダウンロードと導入手順を確認できる

## 非対応範囲

- Chrome Web Store での公開配布は対象外とし、GitHub Releases の zip 配布を前提にする
- popup 内で任意の ArticleShelf URL へ変更する設定は提供しない
- 拡張機能内に password、session cookie、CSRF token、refresh token を保存しない
- 拡張機能専用 token では記事削除、アカウント参照/更新、タグ変更、管理操作を許可しない
- Chrome 内部ページ、拡張機能ページ、blank tab、`file:`、`chrome:`、その他 `http` / `https` 以外のページは記事追加対象にしない
- Firefox、Safari、Edge 固有の拡張機能対応は現時点の対象外とする

## 完了条件

- 拡張機能から `http` / `https` の現在ページを未読または既読として ArticleShelf へ直接保存できる
- 未認証時は ArticleShelf の Web 認証後に拡張機能専用 token を取得できる
- 拡張機能 token は保存用 scope に限定され、24時間で期限切れになる
- ArticleShelf 接続先 URL と API URL は配布物内に固定される
- 配布 zip の作成方法、導入方法、更新方法が利用者と開発者の両方から参照できる
