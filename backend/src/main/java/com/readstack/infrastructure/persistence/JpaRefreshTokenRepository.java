package com.readstack.infrastructure.persistence;

import com.readstack.application.auth.AuthUser;
import com.readstack.application.auth.RefreshTokenRecord;
import com.readstack.application.auth.RefreshTokenRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaRefreshTokenRepository implements RefreshTokenRepository {
    private final SpringDataRefreshTokenJpaRepository refreshTokenJpaRepository;
    private final SpringDataUserJpaRepository userJpaRepository;
    private final JpaAuthUserRepository authUserRepository;

    public JpaRefreshTokenRepository(
            SpringDataRefreshTokenJpaRepository refreshTokenJpaRepository,
            SpringDataUserJpaRepository userJpaRepository,
            JpaAuthUserRepository authUserRepository
    ) {
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.authUserRepository = authUserRepository;
    }

    @Override
    public Optional<RefreshTokenRecord> findByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash).map(this::toApplication);
    }

    @Override
    public RefreshTokenRecord create(AuthUser user, String tokenHash, UUID familyId, Instant expiresAt, String userAgent, String ipAddress) {
        UserEntity userEntity = userJpaRepository.findById(user.id())
                .orElseThrow(() -> new IllegalStateException("refresh token user not found"));
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setUser(userEntity);
        token.setTokenHash(tokenHash);
        token.setFamilyId(familyId);
        token.setExpiresAt(expiresAt);
        token.setUserAgent(userAgent);
        token.setIpAddress(ipAddress);
        return toApplication(refreshTokenJpaRepository.save(token));
    }

    @Override
    public void replace(UUID currentId, UUID replacementId, Instant revokedAt) {
        refreshTokenJpaRepository.findById(currentId).ifPresent(token -> {
            token.setRevokedAt(revokedAt);
            token.setReplacedByTokenId(replacementId);
            refreshTokenJpaRepository.save(token);
        });
    }

    @Override
    public void revoke(UUID id, Instant revokedAt) {
        refreshTokenJpaRepository.findById(id).ifPresent(token -> {
            token.setRevokedAt(revokedAt);
            refreshTokenJpaRepository.save(token);
        });
    }

    @Override
    public void revokeFamily(UUID userId, UUID familyId, Instant revokedAt) {
        refreshTokenJpaRepository.revokeFamily(userId, familyId, revokedAt);
    }

    private RefreshTokenRecord toApplication(RefreshTokenEntity entity) {
        return new RefreshTokenRecord(
                entity.getId(),
                authUserRepository.toApplication(entity.getUser()),
                entity.getFamilyId(),
                entity.getExpiresAt(),
                entity.getRevokedAt()
        );
    }
}
