# Centralized cookie CSRF guard

## 状態

未対応

## 優先度

P2

## 目的

cookie 認証を使う state-changing API が増えた場合に、手動 validator の呼び忘れを防ぐ central filter / annotation ベースの CSRF 防御へ移行できるようにする。

## 対象

- `CsrfTokenValidator`
- refresh / logout 系 API
- Spring Security configuration
- auth / security / API / testing docs

## 対応内容

- 現行の「通常 API は Bearer access token、cookie を使う refresh / logout だけ手動 CSRF」という設計を維持しつつ、cookie 認証対象 API が増える条件を整理する。
- cookie 認証 state-changing endpoint を central filter または annotation で宣言し、CSRF 検証漏れを検出できる構成を検討する。
- Spring Security CSRF を再有効化する場合と、独自 filter に集約する場合の責務境界を比較する。
- refresh / logout の既存 CSRF token / cookie contract と frontend API client の `X-CSRF-Token` 送信を維持する。

## 完了条件

- cookie 認証を使う state-changing API の CSRF 検証が Controller ごとの手動呼び出しに依存しない。
- CSRF 保護対象と対象外の endpoint が security / auth docs で一覧化される。
- CSRF token 不一致、欠落、対象外 Bearer API の回帰が integration test で確認される。
- 将来 cookie 認証 API を追加するときの実装ルールが docs に反映される。

## 根拠

現状の CSRF 設計は、Bearer access token を使う通常 API と cookie を使う refresh / logout を分けており筋が通っている。一方で Spring Security CSRF は無効化されているため、将来 cookie 認証対象 API が増えた場合は手動 validator の適用漏れがリスクになる。
