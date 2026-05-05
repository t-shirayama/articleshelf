# ReadStack Development Setup

ローカル開発環境のセットアップ手順です。

## 前提条件

- Docker / Docker Compose 導入済み
- Java 17+
- Node.js 18+ / npm / yarn
- Git

## 環境構築手順

### 1. リポジトリクローン

```bash
git clone https://github.com/t-shirayama/readstack.git
cd readstack
```

### 2. Docker環境のセットアップ

`docker-compose.yml` を作成して PostgreSQL をコンテナで起動：

```bash
docker-compose up -d
```

**docker-compose.yml** の例:

```yaml
version: "3.8"
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_USER: readstack
      POSTGRES_PASSWORD: dev_password
      POSTGRES_DB: readstack_dev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### 3. バックエンド環境

```bash
cd backend
```

#### 3.1 Spring Boot プロジェクト初期化

Gradle を使う場合:

```bash
gradle init --type java-application --gradle-version current
```

#### 3.2 依存関係の設定

`build.gradle` に以下を追加:

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'

    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.jsoup:jsoup:1.15.3'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}
```

#### 3.3 アプリケーション設定

`src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/readstack_dev
    username: readstack
    password: dev_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  mvc:
    cors:
      allowed-origins: "http://localhost:5173"

server:
  port: 8080
```

#### 3.4 マイグレーション設定

`src/main/resources/db/migration/V1__init.sql`:

```sql
CREATE TABLE articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    url VARCHAR(2048) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    summary TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    read_date DATE,
    favorite BOOLEAN DEFAULT false,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE article_tags (
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, tag_id)
);
```

#### 3.5 起動

```bash
./gradlew bootRun
# または
mvn spring-boot:run
```

サーバーが `http://localhost:8080` で起動します。

### 4. フロントエンド環境

```bash
cd frontend
```

#### 4.1 Vue.js プロジェクト初期化

```bash
npm create vite@latest . -- --template vue
npm install
```

#### 4.2 依存パッケージインストール

```bash
npm install \
  vue-router pinia axios tailwindcss postcss autoprefixer \
  @tailwindcss/forms @headlessui/vue
```

#### 4.3 Tailwind CSS 初期化

```bash
npx tailwindcss init -p
```

#### 4.4 API クライアント設定

`src/api/client.js`:

```javascript
import axios from "axios";

const client = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 10000,
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem("auth_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default client;
```

#### 4.5 開発サーバー起動

```bash
npm run dev
```

アプリが `http://localhost:5173` で起動します。

### 5. データベース初期化

```bash
# 必要に応じて、サンプルタグを挿入
psql -U readstack -d readstack_dev -c "
INSERT INTO tags (name) VALUES ('React'), ('Vue.js'), ('TypeScript'), ('AWS'), ('設計');
"
```

## ローカル開発ワークフロー

### ターミナル1: バックエンド

```bash
cd backend
./gradlew bootRun
```

### ターミナル2: フロントエンド

```bash
cd frontend
npm run dev
```

### ターミナル3: データベース管理（任意）

```bash
psql -U readstack -d readstack_dev
```

## IDE設定

### VS Code 拡張

バックエンド:

- Extension Pack for Java
- Spring Boot Extension Pack
- Gradle for Java

フロントエンド:

- Vetur / Vue - Official
- Tailwind CSS IntelliSense
- ESLint

## トラブルシューティング

**ポート競合**: 8080 または 5432 が使用中の場合

```bash
# 既存プロセスをキル、または別ポートで起動
docker-compose.yml で `ports` を変更
```

**DB接続失敗**: PostgreSQL が起動していることを確認

```bash
docker ps | grep postgres
```

**CORS エラー**: `application.yml` で `allowed-origins` を確認
