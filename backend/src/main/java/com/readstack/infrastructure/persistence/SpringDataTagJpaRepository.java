package com.readstack.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataTagJpaRepository extends JpaRepository<TagEntity, UUID> {
    Optional<TagEntity> findByNameIgnoreCase(String name);
}
