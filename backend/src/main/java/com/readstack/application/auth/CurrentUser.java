package com.readstack.application.auth;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

public record CurrentUser(UUID id, String username, String displayName, List<String> roles, Instant tokenIssuedAt) {
    public CurrentUser(UUID id, String username, String displayName, List<String> roles) {
        this(id, username, displayName, roles, null);
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
