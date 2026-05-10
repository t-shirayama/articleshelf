# Backend observability metrics 追加

## 状態

未対応

## 優先度

中

## 目的

requestId / correlationId、Micrometer metrics、OGP / auth / article 操作の計測を追加し、障害時に原因を切り分けやすくする。

## 対象

- backend request logging
- Micrometer metrics
- OGP fetch latency / failure
- auth login rate limit / unauthorized
- article created / updated counters
- actuator / monitoring docs

## 対応内容

- requestId または correlationId の発行 / 伝播方針を検討する
- OGP fetch duration / failure count を計測する
- auth login rate limited count、unauthorized count などを計測する
- article created / updated などの主要イベント metrics を検討する
- token や個人情報を logs / metrics に含めない方針を明確にする

## 完了条件

- 主要 backend operation の metrics が追加されている
- requestId / correlationId の扱いが決まっている
- actuator / quality / testing docs に観測性の方針が反映されている
- metrics が個人情報や secret を含まないことを確認している

## 根拠

Backend レビューで、Actuator health/info と CI は整っているが、次の運用性強化として requestId、Micrometer metrics、OGP fetch latency / failure rate が有効と指摘されたため。
