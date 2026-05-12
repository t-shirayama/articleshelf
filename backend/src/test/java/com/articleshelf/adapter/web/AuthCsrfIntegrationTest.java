package com.articleshelf.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.articleshelf.application.article.ArticleMetadata;
import com.articleshelf.application.article.ArticleMetadataProvider;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:articleshelf-csrf-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "articleshelf.frontend-origin=http://localhost:5173",
        "articleshelf.auth.access-token-secret=test-articleshelf-access-secret-change-me-please-32bytes",
        "articleshelf.auth.refresh-token-hash-secret=test-articleshelf-refresh-hash-secret-change-me",
        "articleshelf.auth.cookie-secure=false",
        "articleshelf.auth.cookie-same-site=Lax",
        "articleshelf.auth.csrf-enabled=true",
        "articleshelf.auth.initial-user-enabled=false",
        "articleshelf.auth-rate-limit.enabled=false"
})
class AuthCsrfIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ArticleMetadataProvider metadataProvider;

    @Test
    void refreshRequiresMatchingCsrfCookieAndHeader() throws Exception {
        AuthSession session = register(uniqueUsername("refresh-csrf"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(session.refreshCookie(), session.csrfCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(session.refreshCookie(), session.csrfCookie())
                        .header("X-CSRF-Token", "wrong-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(session.refreshCookie(), session.csrfCookie())
                        .header("X-CSRF-Token", session.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("ARTICLESHELF_REFRESH"))
                .andExpect(cookie().exists("ARTICLESHELF_CSRF"));
    }

    @Test
    void logoutRequiresMatchingCsrfCookieAndHeader() throws Exception {
        AuthSession session = register(uniqueUsername("logout-csrf"));

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(session.refreshCookie(), session.csrfCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(session.refreshCookie(), session.csrfCookie())
                        .header("X-CSRF-Token", session.csrfToken()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("ARTICLESHELF_REFRESH", 0))
                .andExpect(cookie().maxAge("ARTICLESHELF_CSRF", 0));
    }

    @Test
    void bearerProtectedArticleApiDoesNotRequireCsrfHeader() throws Exception {
        when(metadataProvider.fetch(anyString()))
                .thenReturn(new ArticleMetadata("Fetched title", "Fetched summary", "", true));
        AuthSession session = register(uniqueUsername("bearer"));

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/%s",
                                  "title": "Bearer protected article",
                                  "summary": "",
                                  "status": "UNREAD",
                                  "favorite": false,
                                  "rating": 0,
                                  "notes": "",
                                  "tags": []
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Bearer protected article"));
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
                .andExpect(cookie().exists("ARTICLESHELF_REFRESH"))
                .andExpect(cookie().exists("ARTICLESHELF_CSRF"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Cookie refreshCookie = result.getResponse().getCookie("ARTICLESHELF_REFRESH");
        Cookie csrfCookie = result.getResponse().getCookie("ARTICLESHELF_CSRF");
        return new AuthSession(json.get("accessToken").asText(), refreshCookie, csrfCookie);
    }

    private String uniqueUsername(String prefix) {
        return (prefix + "-" + UUID.randomUUID().toString().replace("-", "")).substring(0, 24);
    }

    private record AuthSession(String accessToken, Cookie refreshCookie, Cookie csrfCookie) {
        String bearer() {
            return "Bearer " + accessToken;
        }

        String csrfToken() {
            return csrfCookie.getValue();
        }
    }
}
