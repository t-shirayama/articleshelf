# 001: Auth token strategy

## 状態

採用

## 背景

ArticleShelf はユーザーごとに記事、タグ、メモ、既読履歴を分離する。
公開構成では frontend と backend が別 origin になり、ブラウザから REST API を利用するため、短命な API 認証と安全な session 継続を両立する必要がある。

詳細な現行仕様は [認証仕様](../../specs/auth/README.md) と [Token / Cookie / CSRF](../../specs/auth/tokens.md) を正本とする。

## 決定

- API 認証には短命な JWT access token を使う
- refresh token は HttpOnly cookie として扱い、DB には HMAC hash のみ保存する
- refresh / logout 系 API は CSRF protection の対象にする
- access token は frontend のメモリ上に保持し、localStorage には保存しない
- JWT の発行 / 検証は Spring Security JOSE に委譲する

## 代替案

- server-side session: backend 側の session store が必要になり、無料枠構成や stateless API 方針と相性が落ちる
- long-lived JWT only: 実装は単純だが、漏えい時の失効や端末単位の制御が弱くなる
- refresh token を localStorage に保存: XSS 時の被害が大きくなるため採用しない

## トレードオフ

- access token をメモリ保持にすることで XSS 時の永続的な token 露出を避けやすい
- refresh cookie を使うため、CSRF protection と SameSite / Secure 設定を正しく運用する必要がある
- frontend / API が別 site になる公開構成では `SameSite=None; Secure` と CORS / credentials の整合が必要になる

## 今後

- 複数インスタンス運用を強める場合は、rate limit や token rotation 周辺の共有ストアを検討する
- machine-readable API errors や requestId を整備し、認証失敗時の観測性を高める
