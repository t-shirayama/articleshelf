# 002: OGP SSRF protection

## 状態

採用

## 背景

ArticleShelf は URL 追加時に backend が外部サイトへアクセスし、OGP metadata を取得する。
ユーザー入力 URL を backend が直接 fetch するため、SSRF により localhost、private network、cloud metadata endpoint へアクセスしてしまうリスクを抑える必要がある。

具体的な許可 / 拒否条件は [セキュリティ仕様](../../specs/security/README.md) を正本とする。

## 決定

- OGP 取得は backend の infrastructure 層で実装し、application 層は `ArticleMetadataProvider` port 経由で呼び出す
- scheme は `http` / `https` のみ許可する
- DNS 解決後の IP を検証し、loopback、private、link-local、multicast、metadata endpoint などを拒否する
- redirect は自動追従せず、redirect 先 URL も同じ検証を行う
- response body size、Content-Type、timeout、User-Agent を制限する
- 現行の Render Free 運用では `ARTICLESHELF_OGP_REQUIRE_PROXY_IN_PROD=false` とし、OGP fetch は app-level SSRF guard で防御する
- dedicated outbound proxy を用意できる構成へ移行したら、`ARTICLESHELF_OGP_REQUIRE_PROXY_IN_PROD=true` と `ARTICLESHELF_OGP_PROXY_URL` を設定し、proxy 側で metadata endpoint と private network 宛て egress を遮断する

## 代替案

- frontend から OGP を直接取得する: CORS 制約が強く、取得可否が外部サイトに左右される
- OGP 取得を完全に無効化する: セキュリティは単純になるが、記事追加体験が弱くなる
- allowlist 方式にする: 安全性は高いが、任意の技術記事を保存する体験と相性が悪い

## トレードオフ

- SSRF guard により安全性は上がるが、DNS 解決、redirect、IP 判定、timeout など実装とテストの複雑さが増える
- Java HttpClient が接続時に再解決する余地は app だけで完全には消せないため、Render Free 運用では残余リスクを受け入れる
- 一部の正当な URL が拒否される可能性はあるが、公開 backend が外部 fetch する以上、安全側に倒す
- OGP 取得は外部依存のため、失敗時も記事保存体験をどう保つかを継続して設計する必要がある

## 今後

- OGP 取得を DB transaction 外へ出し、外部 HTTP の遅延や失敗を永続化処理から分離する
- HTML parser や charset handling を改善し、外部サイトごとの差分に強くする
- OGP fetch latency / failure metrics を追加し、運用時の切り分けをしやすくする
