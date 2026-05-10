# Frontend CSP と Markdown security tests 強化

## 状態

未対応

## 優先度

P2

## 目的

Markdown sanitization の既存方針を保ちつつ、CSP と追加テストで XSS 耐性をより説明しやすくする。

## 対象

- `frontend/src/features/articles/domain/renderMarkdown.ts`
- `frontend/src/features/articles/components/MarkdownViewer.vue`
- Cloudflare Pages `_headers` または同等の response headers
- Markdown security tests
- security docs

## 対応内容

- Cloudflare Pages で設定する CSP の導入要否を検討する
- `script-src 'self'`、`object-src 'none'`、`base-uri 'self'`、`frame-ancestors 'none'` などの方針を整理する
- Markdown sanitize tests に `data:` image、svg、iframe、style attribute、malformed nested HTML、target blank rel を追加する
- highlight.js の行単位 highlight と block 単位 highlight のトレードオフをコメントまたは docs に残す
- backend security docs と frontend docs の責務を重複しすぎないよう同期する

## 完了条件

- CSP の採用 / 非採用と理由が docs に残っている
- 採用する場合は公開環境で header が設定されている
- Markdown security test が危険タグ、危険属性、危険 scheme を追加で検証している
- Markdown rendering の設計意図が architecture / security docs から説明できる

## 根拠

Frontend レビューで、DOMPurify と markdown-it の設定は強いが、CSP と追加 security tests を合わせるとセキュリティ設計としてさらに説明しやすいと指摘されたため。
