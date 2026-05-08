package com.readstack.infrastructure.persistence;

import com.readstack.application.auth.AuthUser;
import com.readstack.application.auth.AuthUserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAuthUserRepository implements AuthUserRepository {
    private final SpringDataUserJpaRepository userJpaRepository;

    public JpaAuthUserRepository(SpringDataUserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<AuthUser> findById(UUID id) {
        return userJpaRepository.findById(id).map(this::toApplication);
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        return userJpaRepository.findByUsername(username).map(this::toApplication);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(username);
    }

    @Override
    public AuthUser save(AuthUser user) {
        UserEntity entity = user.id() == null
                ? new UserEntity()
                : userJpaRepository.findById(user.id()).orElseGet(UserEntity::new);
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setPasswordHash(user.passwordHash());
        entity.setDisplayName(user.displayName());
        entity.setRole(user.role());
        entity.setStatus(user.status());
        entity.setLastLoginAt(user.lastLoginAt());
        entity.setTokenValidAfter(user.tokenValidAfter());
        return toApplication(userJpaRepository.save(entity));
    }

    AuthUser toApplication(UserEntity entity) {
        return new AuthUser(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getDisplayName(),
                entity.getRole(),
                entity.getStatus(),
                entity.getLastLoginAt(),
                entity.getTokenValidAfter()
        );
    }
}
