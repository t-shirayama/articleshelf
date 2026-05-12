package com.articleshelf.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataExtensionAuthCodeJpaRepository extends JpaRepository<ExtensionAuthCodeEntity, UUID> {
    Optional<ExtensionAuthCodeEntity> findByCodeHash(String codeHash);
}
