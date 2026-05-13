# フロントエンド認証状態

- `features/auth` は username-based API adapter と Pinia store を持つ
- `AuthScreen` は username、表示名、password を扱う
- 未認証で protected route から `/login?returnTo=...` へ遷移した場合、ログイン / 登録のモード切替後も `returnTo` query を維持し、認証成功後は元の遷移先へ戻る
- ユーザー登録時の表示名は任意。空の場合は backend が正規化済み username を表示名として使う
- access token は memory state に保持し、JWT `exp` の decode は `shared/auth/jwt` の純粋 helper、期限前 refresh timer は `features/auth/services/proactiveRefreshTimer` が担当する
- AppSidebar からアカウント設定ダイアログを開ける
- アカウント設定ダイアログではパスワード変更、全端末ログアウト、退会を扱う
- パスワード変更、全端末ログアウト、退会の成功時は auth state と article state をクリアし、ログイン画面へ戻す。全端末ログアウトは API 失敗時も local credentials を消して fail closed にする
- 管理者パスワードリセットは API のみを提供し、管理者 UI は初期スコープ外とする
