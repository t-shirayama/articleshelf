package com.articleshelf.infrastructure.persistence;

import com.articleshelf.application.extension.ExtensionAuthCode;
import com.articleshelf.application.extension.ExtensionAuthCodeRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class JpaExtensionAuthCodeRepository implements ExtensionAuthCodeRepository {
    private final SpringDataExtensionAuthCodeJpaRepository repository;

    public JpaExtensionAuthCodeRepository(SpringDataExtensionAuthCodeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExtensionAuthCode save(ExtensionAuthCode code) {
        return toDomain(repository.save(toEntity(code)));
    }

    @Override
    public Optional<ExtensionAuthCode> findByCodeHash(String codeHash) {
        return repository.findByCodeHash(codeHash).map(this::toDomain);
    }

    @Override
    public void markConsumed(String codeHash, Instant consumedAt) {
        repository.findByCodeHash(codeHash).ifPresent(entity -> {
            entity.setConsumedAt(consumedAt);
            repository.save(entity);
        });
    }

    private ExtensionAuthCodeEntity toEntity(ExtensionAuthCode code) {
        ExtensionAuthCodeEntity entity = new ExtensionAuthCodeEntity();
        entity.setId(code.id());
        entity.setCodeHash(code.codeHash());
        entity.setUserId(code.userId());
        entity.setClientId(code.clientId());
        entity.setExtensionId(code.extensionId());
        entity.setRedirectUri(code.redirectUri());
        entity.setCodeChallenge(code.codeChallenge());
        entity.setCodeChallengeMethod(code.codeChallengeMethod());
        entity.setExpiresAt(code.expiresAt());
        entity.setConsumedAt(code.consumedAt());
        entity.setCreatedAt(code.createdAt());
        return entity;
    }

    private ExtensionAuthCode toDomain(ExtensionAuthCodeEntity entity) {
        return new ExtensionAuthCode(
                entity.getId(),
                entity.getCodeHash(),
                entity.getUserId(),
                entity.getClientId(),
                entity.getExtensionId(),
                entity.getRedirectUri(),
                entity.getCodeChallenge(),
                entity.getCodeChallengeMethod(),
                entity.getExpiresAt(),
                entity.getConsumedAt(),
                entity.getCreatedAt()
        );
    }
}
