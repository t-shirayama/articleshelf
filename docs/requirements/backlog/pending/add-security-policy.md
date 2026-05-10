# SECURITY.md 追加

## 状態

未対応

## 優先度

P3

## 目的

外部の人が脆弱性を見つけた場合の報告方法、公開 issue を避ける方針、対応目安を `SECURITY.md` として明示する。

## 対象

- `SECURITY.md`
- vulnerability reporting flow
- supported versions / scope
- `docs/specs/security/README.md`
- README / docs links

## 対応内容

- repository root に `SECURITY.md` を追加する
- 脆弱性疑いを public issue に書かない方針を明記する
- 報告時に含めてほしい情報を整理する
- 対応確認、影響評価、修正、公開までの大まかな流れを書く
- 個人開発として現実的な連絡手段と対応 SLA の表現を検討する

## 完了条件

- GitHub の security policy として読める `SECURITY.md` がある
- vulnerability reporting の入口が README または security docs から辿れる
- supported scope と報告時に必要な情報が明記されている
- 既存の security specs と役割が重複しすぎていない

## 根拠

Security レビューで、`docs/specs/security/README.md` は実装仕様として良いが、外部の人が脆弱性を見つけたときの報告窓口として repository root の `SECURITY.md` があるとよいと指摘されたため。
