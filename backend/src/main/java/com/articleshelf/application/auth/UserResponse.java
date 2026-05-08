package com.articleshelf.application.auth;

import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String username, String displayName, List<String> roles) {
    public static UserResponse from(AuthUser user) {
        return new UserResponse(
                user.id(),
                user.username(),
                user.displayName(),
                List.of(user.role())
        );
    }
}
