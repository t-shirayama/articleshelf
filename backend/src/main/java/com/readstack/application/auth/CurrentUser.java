package com.readstack.application.auth;

import java.util.List;
import java.util.UUID;

public record CurrentUser(UUID id, String email, String displayName, List<String> roles) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
