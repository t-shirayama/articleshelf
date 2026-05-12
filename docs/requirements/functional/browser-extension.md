# Chrome 拡張機能

## 目的

Chrome 拡張機能は、利用者がブラウザで読んでいる記事ページを、ArticleShelf の記事追加導線へすばやく渡せるようにする。
Web アプリ側の認証、記事追加、OGP 取得、重複確認、保存処理は既存の ArticleShelf 導線を使い、拡張機能自体は現在ページの情報を渡す最小機能に限定する。

## 対象ユーザー

- Chrome を使って技術記事を読んでいる利用者
- ArticleShelf にログイン済み、またはログイン後に記事追加へ復帰できる利用者
- GitHub Releases またはローカル開発環境から配布 zip を取得し、Chrome の Developer mode で拡張機能を読み込める利用者

## 利用者導線

- 利用者は記事ページを開いた状態で拡張機能 popup を開く
- popup で固定された ArticleShelf 接続先 URL を確認できる
- 「ArticleShelf で下書きを開く」操作により、ArticleShelf の記事追加導線が新しいタブで開く
- 未認証の場合はログイン画面を経由し、ログイン後に同じ記事追加導線へ戻る
- 記事追加モーダルでは、拡張機能から渡された URL とタイトルを初期値として確認し、必要に応じてタグ、メモ、おすすめ度、既読日を追加して保存できる

## 対応範囲

- 現在タブの `http` / `https` URL を ArticleShelf へ渡せる
- 現在タブのタイトルが取得できる場合は、記事タイトルの初期値として渡せる
- ArticleShelf の接続先 URL は、本番用とローカル確認用の配布物ごとに固定する
- 拡張機能から開いた追加導線でも、通常の記事追加と同じ認証、URL 検証、OGP 補完、重複 URL 確認、保存処理を使う
- アカウント設定から配布 zip のダウンロードと導入手順を確認できる

## 非対応範囲

- Chrome Web Store での公開配布は対象外とし、GitHub Releases の zip 配布を前提にする
- popup 内で任意の ArticleShelf URL へ変更する設定は提供しない
- 拡張機能内で記事を直接保存しない
- 拡張機能内に access token、refresh token、cookie、CSRF token を保存しない
- Chrome 内部ページ、拡張機能ページ、blank tab、`file:`、`chrome:`、その他 `http` / `https` 以外のページは記事追加対象にしない
- Firefox、Safari、Edge 固有の拡張機能対応は現時点の対象外とする

## 完了条件

- 拡張機能から `http` / `https` の現在ページを ArticleShelf の追加モーダルへ渡せる
- 未認証時でもログイン後に拡張機能経由の追加導線へ復帰できる
- 拡張機能 storage は使わず、ArticleShelf 接続先 URL は配布物内に固定される
- 配布 zip の作成方法、導入方法、更新方法が利用者と開発者の両方から参照できる
