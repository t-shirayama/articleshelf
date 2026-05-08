const apiBaseUrl = normalizeBaseUrl(
  process.env.READSTACK_SAMPLE_API_BASE_URL ||
    process.env.VITE_API_BASE_URL ||
    "http://127.0.0.1:8080",
);
const sampleUrlBase = normalizeBaseUrl(
  process.env.READSTACK_SAMPLE_URL_BASE || apiBaseUrl,
);
const username = process.env.READSTACK_SAMPLE_USERNAME || "sample";
const password = process.env.READSTACK_SAMPLE_PASSWORD || "password123";
const displayName = process.env.READSTACK_SAMPLE_DISPLAY_NAME || "Sample User";

const sampleArticles = [
  {
    key: "architecture",
    title: "ReadStack アーキテクチャ整理メモ",
    summary: "frontend / backend / docs をどう分けるかを確認するサンプル記事。",
    status: "UNREAD",
    favorite: true,
    rating: 5,
    notes: [
      "## 確認ポイント",
      "",
      "- 責務分離",
      "- API 契約",
      "- テスト観点",
    ].join("\n"),
    tags: ["設計", "ReadStack"],
  },
  {
    key: "backend",
    title: "Spring Boot API の例外処理チェック",
    summary: "入力不正、認証、ドメイン例外の扱いを見直すためのサンプル。",
    status: "READ",
    readDate: "2026-05-08",
    favorite: false,
    rating: 4,
    notes: "API エラーは `messages` を通して UI に表示する。",
    tags: ["Backend", "API"],
  },
  {
    key: "frontend",
    title: "Vue 記事詳細フォームの使い勝手",
    summary: "メモ編集、タグ追加、プレビュー表示の確認に使うサンプル。",
    status: "UNREAD",
    favorite: false,
    rating: 3,
    notes: "[Vue](https://vuejs.org/) と Markdown 表示の確認用メモ。",
    tags: ["Frontend", "UI"],
  },
];

async function main() {
  const accessToken = await ensureSampleUser();
  const authHeaders = { Authorization: `Bearer ${accessToken}` };

  for (const article of sampleArticles) {
    await createArticle(authHeaders, article);
  }

  await createStandaloneTag(authHeaders, "未使用サンプル");
  console.log(`Sample data is ready for user "${username}".`);
}

async function ensureSampleUser() {
  const registerResponse = await request("/api/auth/register", {
    method: "POST",
    body: {
      username,
      password,
      displayName,
    },
  });

  if (registerResponse.ok) {
    console.log(`Created sample user "${username}".`);
    return (await registerResponse.json()).accessToken;
  }

  if (registerResponse.status !== 409) {
    throw new Error(
      `Failed to create sample user: ${registerResponse.status} ${await registerResponse.text()}`,
    );
  }

  const loginResponse = await request("/api/auth/login", {
    method: "POST",
    body: {
      username,
      password,
    },
  });

  if (!loginResponse.ok) {
    throw new Error(
      `Sample user "${username}" already exists, but login failed. Set READSTACK_SAMPLE_PASSWORD to the existing password or choose READSTACK_SAMPLE_USERNAME. ` +
        `${loginResponse.status} ${await loginResponse.text()}`,
    );
  }

  console.log(`Using existing sample user "${username}".`);
  return (await loginResponse.json()).accessToken;
}

async function createArticle(authHeaders, article) {
  const response = await request("/api/articles", {
    method: "POST",
    headers: authHeaders,
    body: {
      url: `${sampleUrlBase}/actuator/health?readstackSample=${encodeURIComponent(article.key)}`,
      title: article.title,
      summary: article.summary,
      status: article.status,
      readDate: article.readDate,
      favorite: article.favorite,
      rating: article.rating,
      notes: article.notes,
      tags: article.tags,
    },
  });

  if (response.status === 409) {
    console.log(`Skipped existing article: ${article.title}`);
    return;
  }

  if (!response.ok) {
    throw new Error(
      `Failed to create article "${article.title}": ${response.status} ${await response.text()}`,
    );
  }

  console.log(`Created article: ${article.title}`);
}

async function createStandaloneTag(authHeaders, name) {
  const response = await request("/api/tags", {
    method: "POST",
    headers: authHeaders,
    body: { name },
  });

  if (response.status === 409) {
    console.log(`Skipped existing tag: ${name}`);
    return;
  }

  if (!response.ok) {
    throw new Error(
      `Failed to create standalone tag "${name}": ${response.status} ${await response.text()}`,
    );
  }

  console.log(`Created standalone tag: ${name}`);
}

async function request(path, { method, headers = {}, body }) {
  return fetch(`${apiBaseUrl}${path}`, {
    method,
    headers: {
      "Accept-Language": "ja",
      "Content-Type": "application/json",
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

function normalizeBaseUrl(value) {
  return value.replace(/\/+$/, "");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
