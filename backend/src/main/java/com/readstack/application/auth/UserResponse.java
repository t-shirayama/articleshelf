package com.readstack.application.auth;

import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName, List<String> roles) {
    public static UserResponse from(AuthUser user) {
        return new UserResponse(
                user.id(),
                user.email(),
                user.displayName(),
                List.of(user.role())
        );
    }
}
