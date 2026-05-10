# Distributed auth rate limit

## 状態

未対応

## 優先度

P2

## 目的

`ConcurrentHashMap` ベースの in-memory rate limit を、複数インスタンスや再起動を前提に説明できる構成へ拡張する。

## 対象

- `AuthRateLimiter`
- `AuthAttemptGuard`
- register / login の rate limit
- runtime / security / auth / testing docs

## 対応内容

- 現行の単一インスタンス向け in-memory 実装を維持するか、Redis などの共有ストア、reverse proxy、WAF 側 rate limit のいずれかへ責務を移す方針を決める。
- login は `IP + username`、register は `IP` の制限 key と既存 429 response contract を維持する。
- 複数インスタンス構成で試行回数が分散して回避されないことを検証する。
- 再起動時に rate limit state が消える現状の扱いを、運用上の許容または共有ストア化として明記する。

## 完了条件

- 複数 backend instance でも register / login の rate limit が一貫して効く設計または運用境界がある。
- auth / security / runtime docs に、rate limit の責務、key、共有状態、再起動時の扱いが反映される。
- 429 response、metrics、既存 auth flow が回帰しない。
- 共有ストアを導入する場合は、local dev と production の設定差分、障害時の fail-open / fail-closed 方針が明記される。

## 根拠

現状の `AuthRateLimiter` は `ConcurrentHashMap` ベースで、個人開発や単一インスタンス運用では妥当だが、複数台構成では試行回数が instance ごとに分散し、再起動時にも state が失われる。面接では「複数台ならどうするか」を問われやすいため、将来タスクとして管理する。
