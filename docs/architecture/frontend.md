# Frontend Architecture

## レイヤー構成

- `features/articles`: 記事管理機能の画面、コンポーネント、Pinia store、API adapter、ドメイン helper
- `features/auth`: ユーザー登録 / ログイン画面、認証 API adapter、access token を保持する Pinia store
- `features/articles/views`: 一覧 / カレンダー / 詳細を切り替える feature workspace
- `features/articles/components`: 記事カード、詳細、追加モーダル、フィルタ、サイドバーなど記事機能の UI
- `features/articles/composables`: feature 内の表示状態、画面遷移、記事操作、タグ操作、詳細フォーム、ローテーションなど UI ロジック
- `features/articles/data`: 学習継続カードなど feature 固有の静的データ
- `features/articles/domain`: フィルタ、ソート、フォーム変換、API 入力変換など副作用を持たない関数
- `features/articles/api`: 記事 / タグ API を型付きで呼び出す feature adapter
- `shared`: JWT 付与 / refresh retry 対応の API client、共通 UI、IndexedDB キャッシュ、日付 formatting、i18n messages など機能横断の部品
- `styles`: design token、base、layout、controls、feature styles、responsive を責務単位で分割
- `App.vue`: Vuetify アプリの最上位 shell と feature workspace の接続
- `Vuetify`: ボタン、入力、カード、ダイアログ、チップなどのUIコンポーネント

## 責務分割

- 表示状態と画面操作は feature の view / component に閉じる
- API 通信は feature adapter または shared API client に集約する
- 画面遷移、未保存警告、記事操作、タグ操作、詳細フォーム、タグ管理の検索 / 並び替え / ダイアログ状態など、複数要素にまたがる UI ロジックは feature composable に切り出す
- 検索、フィルタ、ソート、フォーム変換などの純粋処理は `features/articles/domain` に置く
- feature 固有の静的文言や表示候補は `features/articles/data` に置き、component / composable から生成関数越しに参照する
- 画像 Blob キャッシュ、日付 formatting、認証付き fetch など機能横断の処理は `shared` に置く
- feature の表示 component が図形や状態管理を大きく抱える場合は、専用 component / composable に分ける
- i18n 文言は locale ごとに `shared/i18n/messages/` 配下へ分割し、`shared/i18n/messages.ts` は集約 export にする

## UI 方針との関係

- UI の詳細ルールは `docs/designs/README.md` を正とする
- スマホ対応の詳細仕様は `docs/designs/mobile-responsive.md` を参照する
- UI 変更時は design docs とスクリーンショット更新要否を確認する
