# ArticleShelf Agents

このドキュメントは、ArticleShelf を Codex / AI エージェント / 作業者が変更するときの作業者ハンドブックです。
初期リリース完了後は、保守、改善、Backlog 消化、品質向上のための判断基準として扱います。

`docs/README.md` は人間がドキュメント全体を読み始めるための入口です。
`AGENTS.md` は変更時に何を確認し、どの文書を更新し、どの検証を行うかを決める運用ルールです。

## 目的

- 変更の意図、影響範囲、確認結果があとから追える状態を保つ
- 実装、仕様、設計、運用、Backlog の責務を分け、ドキュメントの二重管理を避ける
- 初期リリース後の改善作業を、小さく安全に進める
- レビュー指摘、技術的負債、将来案を Backlog に集約し、優先度付きで扱う

## レビュー観点

作業時は、必要に応じて次の観点で変更内容を確認します。

### Product

- ユーザー体験、導線、主要価値が一貫しているか
- 記事のストック、検索、既読管理、振り返りの流れを壊していないか
- 仕様変更が現在の要件や Backlog と矛盾していないか

### Design

- 見た目、余白、整列、レスポンシブ表示が崩れていないか
- 記事一覧、詳細、タグ管理、追加モーダルの操作が一貫しているか
- UI 変更時に design docs と screenshots の更新要否を判断しているか

### Frontend

- Vue 3 / TypeScript / Pinia / vue-i18n の責務分離を保っているか
- UI 状態、フォーム、フィルタ、ナビゲーション、エラー表示が仕様と合っているか
- 文言、アクセシブル名、セレクタ、テスト期待値が古い前提のまま残っていないか

### Backend

- Spring Boot backend の domain / application / infrastructure / adapter の境界を保っているか
- API 契約、永続化、検索条件、認証、セキュリティ境界が仕様と合っているか
- PostgreSQL 前提の挙動、Flyway migration、Repository 条件を安全に扱っているか

### QA

- 回帰リスク、CI、テスト観点、運用リスクを確認しているか
- 変更種別に合った検証を選び、不要な重い確認を増やしていないか
- 確認できないリスクを、完了報告または Backlog に残しているか

## Codex運用ルール

### 1. 作業開始時

- 依頼範囲を最初に絞り、無関係な修正は巻き取らない
- 変更対象に応じて、関連する `docs/`、`specs/`、`architecture/`、`designs/`、`testing/`、Backlog を確認する
- 実装とドキュメントに差分を見つけた場合は、どちらを正とするか確認する
- その場で確認できない差分は放置せず、関連する仕様、設計、運用文書、または Backlog に現状との差分を残す
- 新機能や大きな仕様変更では、実装前に影響範囲と更新対象を整理する

### 2. 基本原則

- 変更は依頼範囲に集中し、小さく安全な差分にする
- 実装、設計、ドキュメントの整合を保ち、変更の意図があとから追える状態を維持する
- 実装だけを先行させず、必要な仕様、設計、運用文書を同じ作業内で同期する
- 置き換えや仕様変更で不要になったコード、設定、スクリプト、ドキュメント参照は同じ作業内で削除する
- 仕様書や設計書に未反映の実装を追加した場合は、後続タスク扱いにせず、可能な限り同一作業で同期する
- 作業中に今回の範囲外の問題を見つけた場合は、勝手に直さず、必要に応じて Backlog への追記候補として残す

### 3. ドキュメントの正本

- 要件は `docs/requirements/` に置き、何を満たすべきかを書く
- 現行仕様、API 契約、UI 挙動、セキュリティ挙動、品質仕様は `docs/specs/` に置く
- 構造、責務境界、データフロー、永続化方針、実行基盤は `docs/architecture/` に置く
- 見た目、レイアウト判断、コンポーネント設計、レスポンシブ詳細、スクリーンショットは `docs/designs/` に置く
- 検証方針、テストケース、CI 上の確認は `docs/testing/` に置く
- 今後のタスク、構想段階の案、TODO、技術的負債は `docs/requirements/backlog/` に置く

### 4. Docs 構成ルール

- `docs/<area>/` 直下は原則として `README.md` と責務別フォルダだけを置く
- 画像、スクリーンショット、生成キャプチャなどの資産専用フォルダは例外として扱う
- `docs/<area>/<responsibility>/` 配下は、小さな責務なら `README.md` のみで完結させる
- 複数の読者、更新理由、正本が混ざる場合は、`README.md` を索引にして詳細 `.md` を分割する
- README は入口または短い正本要約にし、詳細な重複説明を複数箇所へ置かない

### 5. 変更時のドキュメント更新

- コードを変更した場合は、その変更に影響するドキュメントも同じ作業内で更新する
- コード修正だけで作業を完了せず、関連ドキュメントの更新要否まで確認してから完了報告する
- ドキュメント更新が不要な場合も、完了報告で理由を一言添える
- ドキュメント更新の対象は、変更に直接関係する文書だけにする
- Backlog に書いた内容は、実装時期や仕様が具体化した段階で `docs/specs/README.md` または `docs/specs/` 配下へ反映する

更新対象の判断基準:

- 要件や目的の変更: `docs/requirements/README.md`
- 構想段階の追加案、残作業、TODO、技術的負債: `docs/requirements/backlog/pending/`
- 機能仕様や API 契約の変更: `docs/specs/features/README.md` または `docs/specs/api/README.md`
- 認証アカウント API の変更: `docs/specs/auth/account-api.md`
- セキュリティ対策、認証/認可、CSRF/CORS、secret、rate limit、SSRF、Markdown sanitization の変更: `docs/specs/security/README.md`
- セキュリティ変更の関連文書: 必要に応じて `docs/requirements/non-functional/security.md`、`docs/specs/auth/README.md`、`docs/deployment/README.md`、`docs/testing/README.md`
- 構成、責務分割、データフロー、永続化方針の変更: `docs/architecture/README.md` または `docs/architecture/` 配下の詳細文書
- 画面構成、UI 挙動、操作フロー、見た目の変更: `docs/designs/README.md` と関連 design docs
- 起動方法、開発手順、プロジェクト概要の更新: `README.md`
- Agent や作業ルールの変更: `AGENTS.md` と必要に応じて `.codex/skills/`

### 6. 検証方針

- 変更種別に合った最小限の確認を選ぶ
- docs-only 変更では、build / unit / E2E は不要とし、Markdown link check、old path search、`git diff --check` を優先する
- 既存の UT / IT / E2E が対象機能や UI を検証している場合、コード修正に合わせてテストも同じ作業内で追従させる
- UI 部品の種類、アクセシブル名、文言、操作フロー、API レスポンス、エラー文言を変えたときは、関連するセレクタ、期待値、ヘルパーが古い前提のまま残っていないか確認する
- 挙動変更を伴う修正では、確認手順または確認結果を残す
- コミット前の軽い確認は `.githooks/pre-commit` で実行し、フロントエンド変更時は型チェック、UI 変更時は関連ドキュメント更新の注意喚起を行う
- `.githooks/pre-push` は CI の代替ではなく早期検知用とし、変更パスに応じた targeted unit test / E2E smoke / backend check を担わせる
- API、永続化、検索、状態遷移のように壊れやすい箇所を変更する場合は、回帰確認を優先する
- `mvn` コマンドを使った確認やビルドは、ローカル環境に Maven が入っている前提で実行しない
- Maven が必要な確認は `docker compose exec backend mvn ...` または `docker compose run --rm backend mvn ...` のように Docker 経由で行う
- Spring Data JPA の `@Query`、JPQL、native SQL、Repository の検索条件、Flyway migration、DB 制約を変更した場合は、H2 だけでなく PostgreSQL 実体を使う Docker 経由テストを実行する
- `LIKE`、`concat`、`coalesce`、nullable parameter、enum、UUID、日付、JOIN を含む条件は PostgreSQL との型推論差が出やすいため、persistence IT を追加または更新して確認する

### 7. UI / Design 変更ルール

- UI や操作フローを変更した場合は、`docs/designs/README.md` から関連する design docs を確認し、差分があるなら更新する
- UI や見た目を修正する場合は、実装前に `docs/designs/README.md` のデザイン判断ルール（近接・整列・反復・対比）を参照する
- 完了前に近接・整列・反復・対比の4原則で確認し、入力欄とボタンの右端/左端、セレクト値の見切れ、状態変化時の高さ変化、長い日本語/英語文言の収まりを確認する
- 画面表示、見た目、レイアウト、文言、操作フロー、スクリーンショット対象コンポーネントに影響するコード変更では、同じ作業内で `docs/designs/screenshots/` を現行実装に合わせて更新する
- UI スクリーンショットや `docs/designs/screenshots/` を更新する場合は、`npm run capture:designs` の撮影条件、`docs/designs/screenshots/README.md`、現行 UI 仕様がずれていないか確認する
- UI スクリーンショット更新では、原則として `docker-compose.e2e.yml` を使って `localhost:4173` / `localhost:18080` で起動し、キャプチャはローカルの Playwright から実行する
- `frontend/playwright.config.ts`、`frontend/scripts/e2e-webserver.*`、`docker-compose.e2e.yml` など E2E 基盤を変える場合は、専用 stack 既定と docs を同じ作業内で同期し、`PLAYWRIGHT_USE_EXISTING_SERVER` opt-in と `reuseExistingServer: false` を崩さない
- UI スクリーンショット更新では、`127.0.0.1` と `localhost` を混在させない
- UI スクリーンショット更新で詰まった場合は、先に `docker compose -f docker-compose.e2e.yml ps`、backend health、frontend 応答、`npx playwright install chromium` を確認する
- アプリ本体のコードを疑う前に、起動経路とブラウザ依存を切り分ける
- UI 文言を変更する場合は表記揺れを確認し、非破壊のモーダル終了は「閉じる」、削除確認など確認操作の中止は「キャンセル」と表記する
- UI 表示言語は日本語 / English に対応し、表示文言を追加・変更する場合は `vue-i18n` の翻訳辞書へ反映する
- 未対応言語は英語へフォールバックする
- Vuetify の UI ロケールは現在の表示言語に追従する
- 日付表示は画面上では `ja-JP` / `en-US` に応じた表示、API や永続化で扱う値は既存契約に合わせて `YYYY-MM-DD` を使う
- 日付ピッカーは各ロケールに追従し、日曜は赤系、土曜は青系で表示する
- 新規作成、編集、絞り込みなど操作完了を伴うモーダルは、タイトルと主要アクションを同じヘッダー行に置き、本文末尾に同じアクションを重複配置しない

### 8. Security / Markdown 変更ルール

- API、認証、Cookie、CSRF、CORS、secret、rate limit、SSRF、Markdown sanitization を変更する場合は、`docs/specs/security/README.md` を必ず確認する
- Markdown や `v-html` を扱う場合は raw HTML を無効化し、DOMPurify などでサニタイズする
- Markdown のリンクや画像は許可スキームを明示し、コード非実行の前提を崩さない
- OGP 取得、外部 URL アクセス、redirect、Content-Type、body size、private IP 制限を変更する場合は security spec と testing docs を同期する
- production profile、secret guard、Cookie 属性、CSRF token の責務を変える場合は auth / security / deployment docs を同期する

### 9. Backlog の扱い

- `docs/requirements/backlog/` は、タスク、残作業、構想段階の案、TODO、技術的負債の唯一の記録先として扱う
- Backlog タスクは1ファイル1タスクとし、ファイル名は英小文字 kebab-case の短い名前にする
- タスク本文は `# タスク名`、`## 状態`、`## 優先度`、`## 目的`、`## 対象`、`## 対応内容`、`## 完了条件`、`## 根拠` を標準見出しにする
- Backlog タスク追加時は `P0` から `P4` の優先度を必ず分類し、定義は `docs/requirements/backlog/README.md` の「優先度」に従う
- 状態変更はファイル移動で表す
- 未対応は `docs/requirements/backlog/pending/` に置く
- 対応中は `docs/requirements/backlog/in-progress/` に置く
- 完了時は `docs/requirements/backlog/archive/YYYY-MM-DD.md` へ要約を追記し、タスクファイルは削除する
- `docs/requirements/backlog/README.md` と各状態フォルダの `README.md` は必ず索引として更新する
- 各仕様書、設計書、運用文書には現在の仕様、設計、運用だけを書き、追加対応や構想段階の事項は Backlog タスクへ集約する
- 一時対応、妥協実装、既知の制約を入れた場合は、現在仕様として必要な説明を該当 docs に反映し、残る作業だけ Backlog タスクにする
- ドキュメント内に TODO、TBD、要確認、残作業 が出た場合は、原則 Backlog の具体的なタスクへ変換する
- 作業中に `P0` / `P1` 相当の未実装事項や技術的負債を見つけた場合は、今回の対応範囲外でも `docs/requirements/backlog/pending/` への追記候補として残す

### 10. README の扱い

- `README.md` は初見の開発者が最初に読む前提で保守する
- 起動方法、構成、主要機能、現状説明が古くなった場合は、関連作業の中で更新する
- 現状とずれた予定表現が残っている場合は、実態に合わせて見直す
- 重要な設計判断、運用方式、公開構成が変わった場合は、README から詳細 docs への導線も同期する

### 11. Skill の使い分け

- UI や見た目を修正する場合は、必要に応じて `.codex/skills/articleshelf-ui-polish/SKILL.md` を参照する
- コード変更とドキュメント、確認観点を同期する場合は、必要に応じて `.codex/skills/articleshelf-change-sync/SKILL.md` を参照する
- docs リンク、旧パス、構成ルール、責務重複、AGENTS / skills の同期を確認する場合は、必要に応じて `.codex/skills/articleshelf-docs-audit/SKILL.md` を参照する
- UI スクリーンショット更新では、必要に応じて `.codex/skills/articleshelf-design-capture/SKILL.md` を参照する

### 12. 作業完了時の報告

作業完了時は、少なくとも以下を簡潔に報告する。

- 変更内容
- 更新したドキュメント
- 実行した確認
- 未対応事項またはフォローが必要な点
