# CSP unsafe-inline removal

## 状態

未対応

## 優先度

P2

## 目的

CSP の `style-src 'unsafe-inline'` をなくす、または nonce / hash / framework 設定で許容範囲を狭める道筋を作り、現状許容の理由と次の削減策を説明できるようにする。

## 対象

- Cloudflare Pages `_headers`
- frontend style / Vuetify runtime style
- security / deployment / frontend architecture docs
- CSP regression checks

## 対応内容

- Vuetify runtime style と既存 CSS 運用のどこが inline style を必要としているかを棚卸しする。
- `style-src 'unsafe-inline'` を nonce、hash、`style-src-attr` / `style-src-elem` 分離、または Vuetify 設定で削減できるか検証する。
- local dev、preview、production で CSP header がずれないように deploy docs と capture / E2E 条件を整理する。
- 削減できない inline style が残る場合は、残す理由と削減できる条件を security specs に明記する。

## 完了条件

- production CSP から `style-src 'unsafe-inline'` が削除される、または許容範囲がより狭い directive に分離される。
- Vuetify component、date picker、dialog、responsive layout が CSP 下で崩れない。
- security / deployment docs に現行 CSP と残る制約が反映される。
- 必要に応じて E2E または browser smoke test で CSP violation と表示崩れを確認する。

## 根拠

現行の CSP は全体として強化されているが、`style-src 'unsafe-inline'` が残っている。security spec では Vuetify の runtime style を理由に当面許可しているが、面接では「なぜ許容したか」と「次にどう消すか」を説明する必要がある。
