# OGP HTML 解析と charset 対応改善

## 状態

未対応

## 優先度

P3

## 目的

OGP 取得後の HTML 解析と文字コード判定を堅牢にし、外部サイトごとの HTML 差分や charset 差分に強くする。

## 対象

- `backend/src/main/java/com/articleshelf/infrastructure/ogp/OgpClient.java`
- `backend/src/main/java/com/articleshelf/infrastructure/ogp/OgpService.java`
- OGP HTML parsing
- `Content-Type` charset
- meta charset
- OGP 関連 tests

## 対応内容

- 現行の HTML 解析方式と失敗しやすいパターンを整理する
- `jsoup` などの HTML parser 導入要否を検討する
- `Content-Type` の charset と HTML 内の meta charset を読んで文字コードを扱う方針を検討する
- body size 制限、Content-Type 制限、SSRF guard の前提を維持する
- 外部サイトの代表ケースを使った OGP 抽出確認を追加または整理する

## 完了条件

- HTML parser / charset handling の方針が決まっている
- 必要な場合は OGP parsing 実装が parser ベースに置き換わっている
- UTF-8 固定で壊れるケースの扱いが改善または明示されている
- security specs と OGP 関連 tests が更新されている

## 根拠

Backend レビューで、OGP 取得の SSRF 対策は強い一方、HTML 解析は将来的に `jsoup` などの parser へ寄せ、文字コードも `Content-Type` charset や meta charset を読むと完成度が上がると指摘されたため。
