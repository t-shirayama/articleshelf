# Production profile runtime enforcement

## 状態

未対応

## 優先度

P1

## 目的

本番環境で production profile が必ず有効になり、dev secret、CSRF 無効、cookie secure 無効などの開発用既定値が公開環境へ出ない運用保証を強化する。

## 対象

- Spring production profile validation
- deployment configuration
- Docker / hosting environment variables
- security / deployment / runtime / testing docs

## 対応内容

- production profile の起動ガードが守る値と、production profile 自体が有効でない場合に残るリスクを整理する。
- 公開環境の deploy command / environment で `SPRING_PROFILES_ACTIVE=prod` などの production profile 有効化を必須化する。
- dev secret、`AUTH_CSRF_ENABLED=false`、`COOKIE_SECURE=false`、TLS なし DB URL が production で拒否されることを確認する。
- CI / deployment smoke check で production profile が有効な設定になっているか検証できるようにする。

## 完了条件

- 公開環境で production profile が必ず有効になる設定または検証手順がある。
- production profile 未設定で公開用 deploy しようとした場合に、CI、startup validation、または deployment checklist で検出できる。
- security / deployment / runtime docs に、dev default と production required setting の境界が明記される。
- CSRF、cookie secure、secret、DB TLS の production guard が回帰しない。

## 根拠

現状は production profile の validator が CSRF 無効、弱い secret、TLS 無効 DB URL などを拒否する。一方でデフォルト設定には dev secret、CSRF false、cookie secure false が含まれるため、本番で production profile を必ず有効化する運用保証が重要になる。
