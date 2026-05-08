# Security Boundaries

## Token Boundary

- access token はフロントエンドの Pinia store にメモリ保持する
- refresh token は cookie を使い、ページ再読み込み時や access token 期限切れ時の session 復元に使う
- API client は `Authorization: Bearer` 付与、期限前 refresh、`401` 時の refresh / retry を担当する
- 認証ユースケースは backend application 層の `AuthService` に閉じる
- JWT 発行 / 検証、refresh token hash、password encoder は application 層のポート越しに infrastructure 実装へ委譲する

## Markdown 表示の安全境界

- 詳細画面のメモ Markdown はフロントエンドだけで HTML に変換し、バックエンドには元のメモ本文を保存する
- Markdown 変換では raw HTML を無効化し、ユーザー入力の `<script>` やイベント属性を Markdown として実行可能な HTML にしない
- `v-html` に渡す HTML は必ず DOMPurify を通し、`script` / `iframe` / `object` / `embed` / `style` / フォーム系タグ / SVG / MathML / media 系タグを禁止する
- リンクは `http` / `https` / `mailto` のみ許可し、外部リンクには `target="_blank"` と `rel="noopener noreferrer nofollow"` を付ける
- 画像は `http` / `https` のみ許可し、`data:` や `javascript:` などのスキームは表示しない
- コードブロックは文字列を highlight.js で静的に装飾するだけで、コード本文を評価・実行しない
