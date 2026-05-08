package com.readstack.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readstack.application.article.ArticleMetadata;
import com.readstack.application.article.ArticleMetadataProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:readstack-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "readstack.frontend-origin=http://localhost:5173",
        "readstack.auth.access-token-secret=test-readstack-access-secret-change-me-please-32bytes",
        "readstack.auth.refresh-token-hash-secret=test-readstack-refresh-hash-secret-change-me",
        "readstack.auth.cookie-secure=false",
        "readstack.auth.cookie-same-site=Lax",
        "readstack.auth.csrf-enabled=false",
        "readstack.auth.initial-username=owner-test",
        "readstack.auth.initial-user-password=password123"
})
class AuthAndArticleIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ArticleMetadataProvider metadataProvider;

    @Test
    void protectedArticleApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registeredUsersCanOnlySeeTheirOwnArticles() throws Exception {
        when(metadataProvider.fetch(anyString()))
                .thenReturn(new ArticleMetadata("Fetched title", "Fetched summary", "", true));
        AuthSession userA = register(uniqueUsername("user-a"));
        AuthSession userB = register(uniqueUsername("user-b"));

        MvcResult createResult = mockMvc.perform(post("/api/articles")
                        .header("Authorization", userA.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/%s",
                                  "title": "User A article",
                                  "summary": "",
                                  "status": "UNREAD",
                                  "favorite": false,
                                  "rating": 0,
                                  "notes": "private memo",
                                  "tags": ["private"]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("User A article"))
                .andReturn();
        String articleId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/articles")
                        .header("Authorization", userB.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/articles/{id}", articleId)
                        .header("Authorization", userB.bearer()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/articles/{id}", articleId)
                        .header("Authorization", userA.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("private memo"));
    }

    @Test
    void duplicateArticleUrlIsScopedByUser() throws Exception {
        when(metadataProvider.fetch(anyString()))
                .thenReturn(new ArticleMetadata("", "", "", true));
        String url = "https://example.com/same-" + UUID.randomUUID();
        AuthSession userA = register(uniqueUsername("dup-a"));
        AuthSession userB = register(uniqueUsername("dup-b"));

        createArticle(userA, url, "First");

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", userA.bearer())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson(url, "Duplicate")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messages[0]").value("This URL is already registered."))
                .andExpect(jsonPath("$.existingArticleId").isNotEmpty());

        createArticle(userB, url, "Other user");
    }

    @Test
    void apiErrorsFollowAcceptLanguageAndFallbackToEnglish() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("Accept-Language", "ja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "bad username",
                                  "password": "password123",
                                  "displayName": "Test User"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("ユーザー名の形式が正しくありません。"));

        mockMvc.perform(post("/api/auth/register")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "bad username",
                                  "password": "password123",
                                  "displayName": "Test User"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Username format is invalid."));

        mockMvc.perform(post("/api/auth/register")
                        .header("Accept-Language", "fr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "bad username",
                                  "password": "password123",
                                  "displayName": "Test User"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Username format is invalid."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "bad username",
                                  "password": "password123",
                                  "displayName": "Test User"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Username format is invalid."));
    }

    @Test
    void apiErrorsCoverMalformedInputAndTypeMismatch() throws Exception {
        AuthSession session = register(uniqueUsername("bad"));

        mockMvc.perform(get("/api/articles/not-a-uuid")
                        .header("Authorization", session.bearer())
                        .header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("ID must be a valid UUID."));

        mockMvc.perform(get("/api/articles")
                        .header("Authorization", session.bearer())
                        .header("Accept-Language", "en")
                        .param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Status must be one of: UNREAD, READ."));

        mockMvc.perform(get("/api/articles")
                        .header("Authorization", session.bearer())
                        .header("Accept-Language", "en")
                        .param("favorite", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Favorite must be true or false."));

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", session.bearer())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Request body is invalid."));

        mockMvc.perform(post("/api/tags")
                        .header("Authorization", session.bearer())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Name is required."));
    }

    @Test
    void articleListAppliesRepositoryBackedFilters() throws Exception {
        when(metadataProvider.fetch(anyString()))
                .thenReturn(new ArticleMetadata("", "", "", true));
        AuthSession session = register(uniqueUsername("filter"));

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/vue-%s",
                                  "title": "Vue memo",
                                  "summary": "frontend",
                                  "status": "READ",
                                  "favorite": true,
                                  "rating": 5,
                                  "notes": "pinia note",
                                  "tags": ["Vue"]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());
        createArticle(session, "https://example.com/java-" + UUID.randomUUID(), "Java memo");

        mockMvc.perform(get("/api/articles")
                        .header("Authorization", session.bearer())
                        .param("status", "READ")
                        .param("tag", "vue")
                        .param("search", "pinia")
                        .param("favorite", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Vue memo"));
    }

    @Test
    void usersCanManageTheirOwnTags() throws Exception {
        when(metadataProvider.fetch(anyString()))
                .thenReturn(new ArticleMetadata("", "", "", true));
        AuthSession session = register(uniqueUsername("tags"));

        String unusedTagId = createTag(session, "Unused");
        mockMvc.perform(patch("/api/tags/{id}", unusedTagId)
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Archive\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Archive"))
                .andExpect(jsonPath("$.articleCount").value(0));

        mockMvc.perform(delete("/api/tags/{id}", unusedTagId)
                        .header("Authorization", session.bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/tag-merge-%s",
                                  "title": "Tagged article",
                                  "summary": "",
                                  "status": "UNREAD",
                                  "favorite": false,
                                  "rating": 0,
                                  "notes": "",
                                  "tags": ["Vue", "Frontend"]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());

        JsonNode tags = objectMapper.readTree(mockMvc.perform(get("/api/tags")
                        .header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        String sourceId = tagIdByName(tags, "Frontend");
        String targetId = tagIdByName(tags, "Vue");

        mockMvc.perform(post("/api/tags/{sourceId}/merge", sourceId)
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetTagId\":\"%s\"}".formatted(targetId)))
                .andExpect(status().isNoContent());

        JsonNode mergedTags = objectMapper.readTree(mockMvc.perform(get("/api/tags")
                        .header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Frontend')]").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertThat(mergedTags).hasSize(1);
        assertThat(mergedTags.get(0).get("name").asText()).isEqualTo("Vue");
        assertThat(mergedTags.get(0).get("articleCount").asInt()).isEqualTo(1);
    }

    @Test
    void tagConflictResponsesCoverDuplicateMergeAndDeleteFailures() throws Exception {
        when(metadataProvider.fetch(anyString()))
                .thenReturn(new ArticleMetadata("", "", "", true));
        AuthSession session = register(uniqueUsername("tag-errors"));

        String sourceId = createTag(session, "Source");
        String targetId = createTag(session, "Target");

        mockMvc.perform(patch("/api/tags/{id}", sourceId)
                        .header("Authorization", session.bearer())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Target\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messages[0]").value("Tag already exists."));

        mockMvc.perform(post("/api/tags/{sourceId}/merge", targetId)
                        .header("Authorization", session.bearer())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetTagId\":\"%s\"}".formatted(targetId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messages[0]").value("Choose a different tag to merge into."));

        mockMvc.perform(patch("/api/tags/{id}", UUID.randomUUID())
                        .header("Authorization", session.bearer())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Missing\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Tag was not found."));

        mockMvc.perform(post("/api/tags/{sourceId}/merge", sourceId)
                        .header("Authorization", session.bearer())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Target tag ID is required."));

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/tag-in-use-%s",
                                  "title": "Tagged article",
                                  "summary": "",
                                  "status": "UNREAD",
                                  "favorite": false,
                                  "rating": 0,
                                  "notes": "",
                                  "tags": ["Target"]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/tags/{id}", targetId)
                        .header("Authorization", session.bearer())
                        .header("Accept-Language", "en"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messages[0]").value("Tags in use cannot be deleted."));
    }

    @Test
    void otherUsersCannotUpdateOrDeletePrivateArticles() throws Exception {
        when(metadataProvider.fetch(anyString()))
                .thenReturn(new ArticleMetadata("Fetched title", "Fetched summary", "", true));
        AuthSession userA = register(uniqueUsername("owner"));
        AuthSession userB = register(uniqueUsername("intruder"));

        MvcResult createResult = mockMvc.perform(post("/api/articles")
                        .header("Authorization", userA.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("https://example.com/private-" + UUID.randomUUID(), "Private article")))
                .andExpect(status().isCreated())
                .andReturn();
        String articleId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/articles/{id}", articleId)
                        .header("Authorization", userB.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("https://example.com/private-" + UUID.randomUUID(), "Hijacked article")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/articles/{id}", articleId)
                        .header("Authorization", userB.bearer()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/articles/{id}", articleId)
                        .header("Authorization", userA.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Private article"));
    }

    @Test
    void refreshTokenRotatesAndOldTokenCannotBeReused() throws Exception {
        AuthSession session = register(uniqueUsername("refresh"));

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(session.refreshCookie()))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("READSTACK_REFRESH"))
                .andReturn();
        String refreshedToken = readAccessToken(refreshResult);
        assertThat(refreshedToken).isNotBlank();

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(session.refreshCookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().maxAge("READSTACK_REFRESH", 0));
    }

    @Test
    void accountOperationsInvalidateSessionsAndDeleteAccount() throws Exception {
        String username = uniqueUsername("account");
        AuthSession session = register(username);

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "password123",
                                  "newPassword": "new-password123"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("READSTACK_REFRESH", 0));

        login(username, "password123").andExpect(status().isUnauthorized());
        AuthSession changedSession = login(username, "new-password123")
                .andExpect(status().isOk())
                .andReturnSession();

        mockMvc.perform(post("/api/auth/logout-all")
                        .header("Authorization", changedSession.bearer()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("READSTACK_REFRESH", 0));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(changedSession.refreshCookie()))
                .andExpect(status().isUnauthorized());

        AuthSession finalSession = login(username, "new-password123")
                .andExpect(status().isOk())
                .andReturnSession();
        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", finalSession.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"new-password123\"}"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("READSTACK_REFRESH", 0));

        login(username, "new-password123").andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/articles")
                        .header("Authorization", finalSession.bearer()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanResetPasswordAndRegularUsersCannot() throws Exception {
        String targetUsername = uniqueUsername("target");
        AuthSession regularUser = register(uniqueUsername("regular"));
        register(targetUsername);
        AuthSession admin = login("owner-test", "password123")
                .andExpect(status().isOk())
                .andReturnSession();

        mockMvc.perform(post("/api/admin/users/{username}/password", targetUsername)
                        .header("Authorization", regularUser.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"regular-reset123\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/users/{username}/password", targetUsername)
                        .header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"admin-reset123\"}"))
                .andExpect(status().isNoContent());

        login(targetUsername, "password123").andExpect(status().isUnauthorized());
        login(targetUsername, "admin-reset123").andExpect(status().isOk());
    }

    private AuthSession register(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password123",
                                  "displayName": "Test User"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("READSTACK_REFRESH"))
                .andExpect(jsonPath("$.user.username").value(username))
                .andReturn();
        return new AuthSession(readAccessToken(result), result.getResponse().getCookie("READSTACK_REFRESH"));
    }

    private LoginResult login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andReturn();
        return new LoginResult(result);
    }

    private String uniqueUsername(String prefix) {
        return (prefix + "-" + UUID.randomUUID().toString().replace("-", "")).substring(0, 24);
    }

    private void createArticle(AuthSession session, String url, String title) throws Exception {
        mockMvc.perform(post("/api/articles")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson(url, title)))
                .andExpect(status().isCreated());
    }

    private String createTag(AuthSession session, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tags")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String tagIdByName(JsonNode tags, String name) {
        for (JsonNode tag : tags) {
            if (name.equals(tag.get("name").asText())) {
                return tag.get("id").asText();
            }
        }
        throw new IllegalArgumentException("tag not found: " + name);
    }

    private String articleJson(String url, String title) {
        return """
                {
                  "url": "%s",
                  "title": "%s",
                  "summary": "",
                  "status": "UNREAD",
                  "favorite": false,
                  "rating": 0,
                  "notes": "",
                  "tags": []
                }
                """.formatted(url, title);
    }

    private String readAccessToken(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private record AuthSession(String accessToken, Cookie refreshCookie) {
        String bearer() {
            return "Bearer " + accessToken;
        }
    }

    private class LoginResult {
        private final MvcResult result;

        LoginResult(MvcResult result) {
            this.result = result;
        }

        LoginResult andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            matcher.match(result);
            return this;
        }

        AuthSession andReturnSession() throws Exception {
            return new AuthSession(readAccessToken(result), result.getResponse().getCookie("READSTACK_REFRESH"));
        }
    }
}
