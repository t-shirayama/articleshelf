# Pending Backlog

未対応タスクの索引です。
状態を変更するときは、対象タスクファイルを `in-progress/` または `archive/` の運用に従って移動・削除し、この索引も更新します。

## タスク一覧

### P0

- 該当なし

### P1

- [OGP SSRF DNS rebinding / TOCTOU 対策強化](harden-ogp-ssrf-dns-rebinding.md)
- [OGP 取得の transaction 外分離](move-ogp-fetch-outside-transaction.md)
- [Production DB TLS 起動ガード追加](enforce-production-db-tls.md)
- [Refresh token rotation atomic 化](atomic-refresh-token-rotation.md)
- [セッション Cookie と CSRF 責務の分離](split-session-cookie-csrf-responsibilities.md)

### P2

- [Article / Tag 永続化 Adapter 分割](split-article-tag-persistence-adapters.md)
- [Article ドメインモデル強化](enrich-article-domain-model.md)
- [Article アプリケーションユースケース分割](split-article-application-usecases.md)
- [ArticleCard accessibility 構造整理](fix-article-card-accessibility-structure.md)
- [Article store state ownership 整理](simplify-article-store-state-ownership.md)
- [ArticleWorkspace 責務分割](split-article-workspace-responsibilities.md)
- [Auth アプリケーションユースケース分割](split-auth-application-usecases.md)
- [Backend observability metrics 追加](add-backend-observability-metrics.md)
- [Frontend API client 強化](harden-frontend-api-client.md)
- [Frontend CSP と Markdown security tests 強化](add-frontend-csp-and-markdown-security-tests.md)
- [Frontend focus management と reduced motion 改善](improve-frontend-focus-and-reduced-motion.md)
- [Frontend quality gates 強化](add-frontend-quality-gates.md)
- [Frontend 検索・フィルタ仕様の契約整理](align-frontend-search-filter-contract.md)
- [Machine-readable API エラー導入](introduce-machine-readable-api-errors.md)
- [PDF インポート実データ品質改善](pdf-import-real-data-quality-improvement.md)
- [Supply chain security scans 追加](add-supply-chain-security-scans.md)
- [Vue Router による画面遷移と URL 状態同期](add-vue-router-workspace-navigation.md)
- [Web adapter 境界の責務整理](cleanup-web-adapter-boundaries.md)
- [カレンダー domain helper と keyboard 操作改善](improve-calendar-domain-and-keyboard.md)
- [記事一覧 pagination / query model 導入](add-article-pagination-query-model.md)
- [Frontend 認証 session contract 整理](cleanup-frontend-auth-session-contract.md)
- [Backend Docker image non-root 実行](run-backend-container-as-non-root.md)
- [認証インフラ境界と運用シグナル強化](harden-auth-infrastructure-boundaries.md)

### P3

- [Backend quality gates の見せ方整理](document-backend-quality-gates.md)
- [Flyway baseline migration の意図整理](document-or-simplify-flyway-baseline.md)
- [Frontend app providers 分離](split-frontend-app-providers.md)
- [Frontend architecture highlights 整理](document-frontend-architecture-highlights.md)
- [OGP HTML 解析と charset 対応改善](improve-ogp-parsing-charset-handling.md)
- [SECURITY.md 追加](add-security-policy.md)
- [TimeProvider / IdGenerator port 導入](introduce-time-id-provider-ports.md)
- [Thumbnail cache eviction 導入](add-thumbnail-cache-eviction.md)
- [TagManagement DOM 計測責務分離](split-tag-management-measured-width.md)
- [記事詳細・追加フォーム component 分割](split-article-detail-and-create-components.md)

### P4

- 該当なし
