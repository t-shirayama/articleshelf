# フロントエンド認証状態

- `features/auth` は username-based API adapter と Pinia store を持つ
- `AuthScreen` は username、表示名、password を扱う
- AppSidebar からアカウント設定ダイアログを開ける
- アカウント設定ダイアログではパスワード変更、全端末ログアウト、退会を扱う
- パスワード変更、全端末ログアウト、退会の成功時は auth state と article state をクリアし、ログイン画面へ戻す
- 管理者パスワードリセットは API のみを提供し、管理者 UI は初期スコープ外とする
