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
    public Optional<AuthUser> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(this::toApplication);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public AuthUser save(AuthUser user) {
        UserEntity entity = user.id() == null
                ? new UserEntity()
                : userJpaRepository.findById(user.id()).orElseGet(UserEntity::new);
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setDisplayName(user.displayName());
        entity.setRole(user.role());
        entity.setStatus(user.status());
        entity.setLastLoginAt(user.lastLoginAt());
        return toApplication(userJpaRepository.save(entity));
    }

    AuthUser toApplication(UserEntity entity) {
        return new AuthUser(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getDisplayName(),
                entity.getRole(),
                entity.getStatus(),
                entity.getLastLoginAt()
        );
    }
}
