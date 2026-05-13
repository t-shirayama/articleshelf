package com.articleshelf.infrastructure.persistence;

import com.articleshelf.application.extension.ExtensionAccessToken;
import com.articleshelf.application.extension.ExtensionAccessTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaExtensionAccessTokenRepository implements ExtensionAccessTokenRepository {
    private final SpringDataExtensionAccessTokenJpaRepository repository;

    public JpaExtensionAccessTokenRepository(SpringDataExtensionAccessTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExtensionAccessToken save(ExtensionAccessToken token) {
        return toDomain(repository.save(toEntity(token)));
    }

    @Override
    public Optional<ExtensionAccessToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    private ExtensionAccessTokenEntity toEntity(ExtensionAccessToken token) {
        ExtensionAccessTokenEntity entity = new ExtensionAccessTokenEntity();
        entity.setId(token.id());
        entity.setTokenHash(token.tokenHash());
        entity.setUserId(token.userId());
        entity.setClientId(token.clientId());
        entity.setExtensionId(token.extensionId());
        entity.setScopes(token.scopes());
        entity.setExpiresAt(token.expiresAt());
        entity.setRevokedAt(token.revokedAt());
        entity.setCreatedAt(token.createdAt());
        return entity;
    }

    private ExtensionAccessToken toDomain(ExtensionAccessTokenEntity entity) {
        return new ExtensionAccessToken(
                entity.getId(),
                entity.getTokenHash(),
                entity.getUserId(),
                entity.getClientId(),
                entity.getExtensionId(),
                entity.getScopes(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getCreatedAt()
        );
    }
}
