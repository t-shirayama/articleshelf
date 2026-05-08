package com.readstack.application.auth;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository {
    Optional<AuthUser> findById(UUID id);

    Optional<AuthUser> findByUsername(String username);

    boolean existsByUsername(String username);

    AuthUser save(AuthUser user);
}
