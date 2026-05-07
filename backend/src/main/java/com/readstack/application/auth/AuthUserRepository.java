package com.readstack.application.auth;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository {
    Optional<AuthUser> findById(UUID id);

    Optional<AuthUser> findByEmail(String email);

    boolean existsByEmail(String email);

    AuthUser save(AuthUser user);
}
