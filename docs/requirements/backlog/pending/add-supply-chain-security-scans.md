# Supply chain security scans 追加

## 状態

未対応

## 優先度

P2

## 目的

Dependency Review、CodeQL、OSV / npm audit / OWASP Dependency-Check、Trivy などを検討し、公開アプリとして依存関係とコンテナの脆弱性検知を強化する。

## 対象

- `.github/dependabot.yml`
- `.github/workflows/ci.yml`
- frontend npm dependencies
- backend Maven dependencies
- Docker images
- GitHub Actions dependencies

## 対応内容

- 既存 Dependabot 設定を確認し、npm / Maven / GitHub Actions / Docker / docker-compose の更新 PR が出る状態を維持する
- GitHub Dependency Review を PR check に追加する
- CodeQL の Java / JavaScript / TypeScript scan を追加する
- OSV Scanner、npm audit、OWASP Dependency-Check の導入要否を比較する
- Trivy で Docker image / filesystem scan を追加する
- third-party GitHub Actions の SHA pinning 方針を検討する

## 完了条件

- 依存関係脆弱性を PR または CI で検知できる
- CodeQL または同等の static analysis が設定されている
- Docker image / filesystem scan の方針が決まっている
- Dependabot との役割分担が docs に反映されている
- CI / testing / security docs が更新されている

## 根拠

Security レビューで、Dependabot はあるものの、Dependency Review、CodeQL、OSV / npm audit / OWASP Dependency-Check、Trivy などの依存関係・コンテナスキャンが薄いと指摘されたため。
