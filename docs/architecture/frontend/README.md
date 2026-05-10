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
- `app/providers`: Pinia、i18n、Vuetify theme / defaults / locale messages など app-level provider 設定
- `shared`: JWT 付与 / refresh retry 対応の API client、共通 UI、IndexedDB キャッシュ、日付 formatting、i18n messages など機能横断の部品
- `styles`: design token、base、layout、controls、feature styles、responsive を責務単位で分割
- `App.vue`: Vuetify アプリの最上位 shell と feature workspace の接続
- `Vuetify`: ボタン、入力、カード、ダイアログ、チップなどのUIコンポーネント

## 責務分割

- 表示状態と画面操作は feature の view / component に閉じる
- API 通信は feature adapter または shared API client に集約する
- API / 通信エラーの HTTP status、API `messages`、重複記事 ID、refresh retry、通信失敗、5xx の汎用化は `shared/api/client` が担当する
- store / composable は `shared/errors` の `errorMessage` で表示文言を取り出し、一覧、タグ管理、追加モーダル、詳細画面など表示先に応じた error state へ入れる
- API client は 5xx や malformed success response の内部詳細を画面へ出さず、i18n の汎用メッセージに変換する
- 画面遷移、未保存警告、記事操作、タグ操作、詳細フォーム、タグ管理の検索 / 並び替え / ダイアログ状態など、複数要素にまたがる UI ロジックは feature composable に切り出す
- `useArticlesStore` の記事一覧 state は `articles` を canonical source とし、検索 / フィルタ / ソート後の一覧は getter と `features/articles/domain` の純粋関数で派生させる
- optimistic update / rollback は canonical な `articles` と `selectedArticle` だけを復元対象にし、同じ記事一覧を別配列で二重保持しない
- 検索、フィルタ、ソート、フォーム変換などの純粋処理は `features/articles/domain` に置く
- feature 固有の静的文言や表示候補は `features/articles/data` に置き、component / composable から生成関数越しに参照する
- 画像 Blob キャッシュ、日付 formatting、認証付き fetch など機能横断の処理は `shared` に置く
- feature の表示 component が図形や状態管理を大きく抱える場合は、専用 component / composable に分ける
- i18n 文言は locale ごとに `shared/i18n/messages/` 配下へ分割し、`shared/i18n/messages.ts` は集約 export にする

## Design Highlights

- feature-oriented: `features/articles` と `features/auth` に画面、API adapter、store、composable、domain helper をまとめ、機能内の変更理由を近くに置く
- app providers: `main.ts` は Vue app 作成と provider 登録に集中し、Pinia、i18n、Vuetify の設定は `app/providers` に分ける
- article form split: 詳細ページの閲覧セクション、メモ編集 / preview、追加モーダルの create form state は dedicated component / composable に分ける
- shared boundary: 認証付き fetch、共通 UI、i18n、日付 formatting、IndexedDB cache のような横断処理だけを `shared` に置く
- auth-aware API client: `shared/api/client` が access token 付与、CSRF header、401 後の refresh retry、API error mapping、malformed response の汎用エラー化を担う
- auth session helpers: JWT `exp` decode は `shared/auth/jwt`、期限前 refresh timer は `features/auth/services/proactiveRefreshTimer` に分け、Pinia store は認証 state と API action に集中する
- cancellable requests: `shared/api/client` は `AbortSignal` を `fetch` へ渡せるため、検索や画面遷移で古い request を中断する導線を作れる
- client-side domain helpers: 検索、フィルタ、ソート、フォーム変換、Markdown rendering などは `features/articles/domain` の副作用を持たない関数へ寄せる
- UI measurement: タグ管理の select 幅など DOM 計測が必要な処理は dedicated composable に分け、タグ管理 state と DOM 依存を混ぜない
- safe Markdown rendering: `renderMarkdown` は raw HTML を無効化した MarkdownIt 出力を DOMPurify で sanitization し、許可スキームや危険タグの境界は [セキュリティ仕様](../../specs/security/README.md) に従う
- responsive UX: desktop / tablet / mobile の見た目と操作は design docs を正本にし、代表導線は Playwright E2E と screenshot capture で確認する

## UI 方針との関係

- UI の共通方針は `docs/designs/README.md`、画面別コンポーネントルールは `docs/designs/components/README.md` を参照する
- デスクトップ、ノート PC、タブレット、スマホのレスポンシブ仕様は `docs/designs/responsive/README.md` を参照する
- スマホ固有の詳細仕様は `docs/designs/responsive/mobile.md` を参照する
- UI 変更時は design docs と `docs/designs/screenshots/README.md` の更新要否を確認する
