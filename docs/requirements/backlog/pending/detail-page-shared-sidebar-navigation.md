# Detail page shared sidebar navigation

## 状態

未対応

## 優先度

P2

## 目的

記事詳細画面でもトップページと同じサイドメニュー導線を見せ、一覧画面と詳細画面の切り替えでレイアウトや操作起点が大きく変わらないようにする。

## 対象

- `frontend/src/features/articles/views/ArticleWorkspace.vue`
- 記事詳細画面の shell / layout
- sidebar / mobile drawer / bottom navigation と detail page の関係
- frontend architecture / designs / UI specs / testing docs

## 対応内容

- 詳細画面でも desktop ではトップページと同じ sidebar を維持し、一覧・カレンダー・タグ管理・アカウント・ログアウトの導線を同じ位置で扱えるようにする。
- mobile では既存の header / drawer / bottom navigation との整合を確認し、詳細表示中にどの導線を見せるかを整理する。
- 未保存確認、詳細の戻る導線、カレンダーから開いた詳細の return view、duplicate article open の挙動を壊さない。
- detail page の幅、余白、スクロール責務を shell 変更後も安定させる。

## 完了条件

- 記事詳細画面でトップページと同じ shell 文脈の中に sidebar または同等の navigation が表示される。
- 一覧 / 詳細 / カレンダー / タグ管理で navigation の位置と意味が一貫する。
- 未保存確認、詳細保存、削除、ログアウト、アカウント設定の導線が回帰しない。
- design docs / UI specs / frontend architecture / testing docs に shell 境界と確認観点が反映される。

## 根拠

現在は一覧・カレンダー・タグ管理では共通の shell が見える一方、記事詳細へ入るとサイドメニューが消えて別画面感が強くなる。操作起点の一貫性と学習コストの低減のため、詳細画面側にも共通ナビゲーションを持たせる改善余地がある。
