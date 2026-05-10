# Router level route boundaries

## 状態

未対応

## 優先度

P1

## 目的

Vue Router の route 定義を placeholder component から実画面単位の route component へ移行し、面接やレビューで route-level component、`router-view`、navigation guard、lazy loading の採用理由を説明できる構成にする。

## 対象

- `frontend/src/app/providers/router.ts`
- `frontend/src/App.vue`
- 認証画面、記事一覧、記事詳細、カレンダー、タグ管理、アカウント設定の route 境界
- frontend architecture / UI / testing docs

## 対応内容

- `/login`、`/register`、`/articles`、`/articles/:id`、`/calendar`、`/tags`、`/settings` に route-level component を割り当てる。
- `App.vue` は app shell、provider 初期化、`router-view` の配置に寄せる。
- 認証済み / 未認証の遷移制御を route guard に移し、画面 component 側の route 補正責務を減らす。
- route component は lazy loading し、初期 bundle に全画面を載せない構成にする。
- URL 直打ち、戻る / 進む、未保存編集の確認、ログアウト後の遷移を既存 UX と合わせて検証する。

## 完了条件

- router 定義から placeholder component がなくなる。
- 各主要画面が route-level component として表示され、`router-view` 経由で切り替わる。
- 認証 route と protected route の制御が navigation guard で説明できる。
- frontend architecture、UI specs、testing docs に route 境界と検証観点が反映される。
- 既存 E2E の主要 navigation、直接 URL アクセス、認証前後の redirect が通る。

## 根拠

現状は route path は存在するが component が placeholder で、実際の画面切り替えは `App.vue` と `ArticleWorkspace.vue` の状態管理に残っている。動作上は成立しているものの、構造設計の説明では route-level 境界が弱く見えやすいため、将来の改善タスクとして明示する。
