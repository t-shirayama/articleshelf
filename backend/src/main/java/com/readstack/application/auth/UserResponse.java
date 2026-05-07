package com.readstack.application.auth;

import com.readstack.infrastructure.persistence.UserEntity;

import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName, List<String> roles) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                List.of(user.getRole())
        );
    }
}
