package com.readstack.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readstack.application.article.ArticleMetadata;
import com.readstack.application.article.ArticleMetadataProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "readstack.frontend-origin=http://localhost:5173",
        "readstack.auth.access-token-secret=test-readstack-access-secret-change-me-please-32bytes",
        "readstack.auth.refresh-token-hash-secret=test-readstack-refresh-hash-secret-change-me",
        "readstack.auth.cookie-secure=false",
        "readstack.auth.cookie-same-site=Lax",
        "readstack.auth.csrf-enabled=false",
        "readstack.auth.initial-user-email=owner-test@example.com",
        "readstack.auth.initial-user-password=password123"
})
class AuthAndArticleIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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
        AuthSession userA = register("user-a-" + UUID.randomUUID() + "@example.com");
        AuthSession userB = register("user-b-" + UUID.randomUUID() + "@example.com");

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
        AuthSession userA = register("dup-a-" + UUID.randomUUID() + "@example.com");
        AuthSession userB = register("dup-b-" + UUID.randomUUID() + "@example.com");

        createArticle(userA, url, "First");

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", userA.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson(url, "Duplicate")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.existingArticleId").isNotEmpty());

        createArticle(userB, url, "Other user");
    }

    @Test
    void refreshTokenRotatesAndOldTokenCannotBeReused() throws Exception {
        AuthSession session = register("refresh-" + UUID.randomUUID() + "@example.com");

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

    private AuthSession register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123",
                                  "displayName": "Test User"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("READSTACK_REFRESH"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();
        return new AuthSession(readAccessToken(result), result.getResponse().getCookie("READSTACK_REFRESH"));
    }

    private void createArticle(AuthSession session, String url, String title) throws Exception {
        mockMvc.perform(post("/api/articles")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson(url, title)))
                .andExpect(status().isCreated());
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
}
