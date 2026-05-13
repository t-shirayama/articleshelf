package com.articleshelf.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataExtensionAccessTokenJpaRepository extends JpaRepository<ExtensionAccessTokenEntity, UUID> {
    Optional<ExtensionAccessTokenEntity> findByTokenHash(String tokenHash);
}
