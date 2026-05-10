# OGP SSRF DNS rebinding / TOCTOU 対策強化

## 状態

未対応

## 優先度

P1

## 目的

OGP 取得時の SSRF 対策を、DNS 検証後に接続先が変わる DNS rebinding / TOCTOU リスクにも強い形へ改善する。

## 対象

- `backend/src/main/java/com/articleshelf/infrastructure/ogp/OgpRequestGuard.java`
- `backend/src/main/java/com/articleshelf/infrastructure/ogp/OgpClient.java`
- OGP redirect handling
- OGP SSRF tests
- `docs/specs/security/README.md`

## 対応内容

- 現行の DNS 解決、IP 検証、HttpClient 接続の流れを確認する
- 検証時と接続時で DNS 応答が変わる場合の拒否方針を検討する
- 接続直前の再解決、検証済み IP への接続、専用 HTTP client / resolver、outbound proxy、インフラ側 egress 制御の選択肢を比較する
- redirect ごとの再検証と DNS rebinding 対策の組み合わせを整理する
- private / metadata endpoint への接続ができないことをテストで確認する

## 完了条件

- OGP fetch で DNS rebinding / TOCTOU リスクへの対策方針が実装または明記されている
- private IP、metadata endpoint、redirect 先再検証、DNS 応答変化のテストが追加または更新されている
- security specs と architecture ADR が必要に応じて更新されている
- 既存の OGP 取得 UX と timeout / body size / Content-Type 制限が維持されている

## 根拠

Security レビューで、`OgpRequestGuard` が `InetAddress.getAllByName(host)` で一度 DNS 解決して検証した後、`HttpClient` が通常の URI 接続を行うため、検証時と接続時で DNS 応答が変わる DNS rebinding / TOCTOU の余地があると指摘されたため。
