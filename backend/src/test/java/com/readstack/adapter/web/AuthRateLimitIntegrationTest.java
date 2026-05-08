package com.readstack.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:readstack-rate-limit-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
        "readstack.auth.initial-user-enabled=false",
        "readstack.auth-rate-limit.enabled=true",
        "readstack.auth-rate-limit.login-capacity=1",
        "readstack.auth-rate-limit.login-window-seconds=60",
        "readstack.auth-rate-limit.register-capacity=1",
        "readstack.auth-rate-limit.register-window-seconds=600"
})
class AuthRateLimitIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerReturnsTooManyRequestsWhenIpLimitIsExceeded() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "203.0.113.20, 10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueUsername("first"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "203.0.113.20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueUsername("second"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.messages[0]").value("Too many authentication attempts. Please wait and try again."));
    }

    @Test
    void loginReturnsTooManyRequestsWhenIpAndUsernameLimitIsExceeded() throws Exception {
        String username = uniqueUsername("login");
        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "203.0.113.21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "203.0.113.22")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, "wrong-password123")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "203.0.113.22")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username.toUpperCase(), "wrong-password123")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.messages[0]").value("Too many authentication attempts. Please wait and try again."));
    }

    private String registerJson(String username) throws Exception {
        return objectMapper.writeValueAsString(new RegisterPayload(username, "password123", "Test User"));
    }

    private String loginJson(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginPayload(username, password));
    }

    private String uniqueUsername(String prefix) {
        return (prefix + "-" + UUID.randomUUID().toString().replace("-", "")).substring(0, 24);
    }

    private record RegisterPayload(String username, String password, String displayName) {
    }

    private record LoginPayload(String username, String password) {
    }
}
