# OGP egress network isolation

## 状態

未対応

## 優先度

P2

## 目的

OGP SSRF 対策を application-level URL / DNS / IP 検証だけに依存せず、network egress control も含めた多層防御として説明できる構成にする。

## 対象

- OGP fetch infrastructure
- deployment network policy
- Docker / production runtime configuration
- SSRF security specs / ADR / testing docs

## 対応内容

- 現行の DNS 再検証、redirect 再検証、metadata endpoint 拒否に加えて、egress firewall、metadata endpoint 遮断、専用 proxy、IP pinning の選択肢を評価する。
- Java HttpClient が検証後に再解決する余地を threat model として docs に明記する。
- デプロイ構成を変えない範囲で可能な遮断策と、cloud / container runtime 側で行うべき遮断策を分ける。
- OGP fetch の timeout、body size、content-type、redirect 制限との責務分担を整理する。

## 完了条件

- SSRF 対策が application validation と network egress control の二層で説明できる。
- cloud metadata endpoint や private network への outbound access を runtime / network layer でも遮断する方針がある。
- security specs / ADR / deployment docs に、残る TOCTOU リスクと運用上の補完策が反映される。
- 可能な範囲で egress control の smoke test または運用確認手順が testing docs に追加される。

## 根拠

現行の OGP SSRF 対策は強く、DNS 再検証や危険 IP 拒否を備えている。ただし DNS 検証から Java HttpClient 接続までの間に再解決される余地は残るため、さらに上を目指す場合は egress firewall、metadata endpoint 遮断、IP pinning、専用 proxy などの多層防御が候補になる。
