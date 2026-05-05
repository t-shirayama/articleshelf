# ReadStack Test Strategy

テスト戦略と実装ガイドです。

## テスト階層

```
┌─────────────────────────┐
│   E2E Test              │ (Cypress / Playwright)
│   ユーザーシナリオ検証  │
└─────────────┬───────────┘
              │
┌─────────────▼───────────┐
│  Integration Test       │ (API + DB)
│  API + UI 統合テスト    │
└─────────────┬───────────┘
              │
┌─────────────▼───────────┐
│  Unit Test              │ (JUnit5, Vitest)
│  個別機能テスト         │
└─────────────────────────┘
```

## バックエンド テスト

### ユニットテスト (JUnit5)

**目標**: ドメインロジック、アプリケーションサービスのテスト

```java
// src/test/java/com/readstack/domain/article/ArticleTest.java
@DisplayName("Article")
class ArticleTest {

    @Test
    @DisplayName("新しい記事を作成できる")
    void should_create_new_article() {
        // Arrange
        String url = "https://zenn.dev/example";
        String title = "Test Article";

        // Act
        Article article = Article.create(url, title);

        // Assert
        assertThat(article.getUrl()).isEqualTo(url);
        assertThat(article.getStatus()).isEqualTo(ArticleStatus.UNREAD);
    }

    @Test
    @DisplayName("記事を読了状態に変更できる")
    void should_mark_article_as_read() {
        // Arrange
        Article article = Article.create("https://example.com", "Title");

        // Act
        article.markAsRead();

        // Assert
        assertThat(article.getStatus()).isEqualTo(ArticleStatus.READ);
    }
}
```

実行:

```bash
./gradlew test
```

### 統合テスト (TestRestTemplate)

**目標**: API エンドポイント + DB の統合確認

```java
// src/test/java/com/readstack/adapter/web/ArticleControllerTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ArticleControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("GET /api/articles で記事一覧が取得できる")
    void should_get_articles() {
        // Act
        ResponseEntity<ArticleResponse[]> response =
            restTemplate.getForEntity("/api/articles", ArticleResponse[].class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }
}
```

実行:

```bash
./gradlew test --tests "*ControllerTest"
```

## フロントエンド テスト

### ユニットテスト (Vitest)

**目標**: コンポーネント、ユーティリティ関数のテスト

```javascript
// src/stores/article.test.js
import { describe, it, expect, beforeEach } from "vitest";
import { setActivePinia, createPinia } from "pinia";
import { useArticleStore } from "./article";

describe("Article Store", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it("新しい記事をストアに追加できる", () => {
    const store = useArticleStore();
    const article = { id: 1, title: "Test", url: "https://example.com" };

    store.addArticle(article);

    expect(store.articles).toContain(article);
  });
});
```

実行:

```bash
npm run test
```

### E2E テスト (Cypress)

**目標**: ユーザーフロー全体の動作確認

```javascript
// cypress/e2e/article.cy.js
describe("記事管理フロー", () => {
  beforeEach(() => {
    cy.visit("http://localhost:5173");
  });

  it("記事を追加して詳細表示できる", () => {
    // 1. 追加ボタンをクリック
    cy.contains("記事を追加").click();

    // 2. フォーム入力
    cy.get('input[placeholder="URL"]').type("https://zenn.dev/test");
    cy.get('input[placeholder="タイトル"]').type("Test Article");
    cy.get('button[type="submit"]').click();

    // 3. 記事が一覧に表示される
    cy.contains("Test Article").should("be.visible");

    // 4. 記事をクリックして詳細表示
    cy.contains("Test Article").click();
    cy.contains("https://zenn.dev/test").should("be.visible");
  });

  it("タグでフィルタリングできる", () => {
    // タグをクリック
    cy.contains("React").click();

    // React タグを持つ記事のみ表示
    cy.get('[data-test="article-card"]').each((card) => {
      cy.wrap(card).contains("React").should("exist");
    });
  });
});
```

実行:

```bash
npm run test:e2e
# またはCypress UIで実行
npx cypress open
```

## カバレッジ目標

| レイヤー                   | 目標 |
| -------------------------- | ---- |
| Domain (ビジネスロジック)  | 90%+ |
| Application (ユースケース) | 80%+ |
| Adapter (API)              | 70%+ |
| Infrastructure             | 60%+ |
| フロントエンド (ロジック)  | 70%+ |

## CI/CD 統合（将来予定）

```yaml
# .github/workflows/test.yml
name: Test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
    steps:
      - uses: actions/checkout@v3
      - run: ./gradlew test
      - run: npm ci && npm run test:e2e
```

## テスト実行コマンド

```bash
# バックエンド全テスト
./gradlew test

# フロントエンド ユニットテスト
npm run test

# E2E テスト
npm run test:e2e

# カバレッジレポート
./gradlew test jacocoTestReport
npm run test:coverage
```
