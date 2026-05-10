# Backend Docker image non-root 実行

## 状態

未対応

## 優先度

P2

## 目的

Backend production Docker image を non-root user で実行し、コンテナ実行時の権限を最小化する。

## 対象

- `backend/Dockerfile`
- `docker-compose.yml`
- `docker-compose.e2e.yml`
- Render deployment
- backend build / runtime tests

## 対応内容

- final image で app user / group を作成する
- jar と working directory の権限を app user で実行できるようにする
- `USER app` を指定する
- local docker compose / e2e compose / Render の起動と health check に影響がないか確認する
- Docker image scan や deployment docs と合わせて説明する

## 完了条件

- backend final Docker image が root ではなく dedicated app user で起動する
- local / e2e / production profile の起動手順が維持されている
- health check と backend logs が従来どおり確認できる
- deployment / security docs が更新されている

## 根拠

Security レビューで、backend の final Docker image は `eclipse-temurin:21-jre-alpine` から jar をコピーして `java -jar` しているが、`USER` 指定がなく root 実行になっていると指摘されたため。
