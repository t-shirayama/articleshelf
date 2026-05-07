package com.readstack.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataTagJpaRepository extends JpaRepository<TagEntity, UUID> {
    List<TagEntity> findAllByUserId(UUID userId);

    Optional<TagEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<TagEntity> findByUserIdAndNameIgnoreCase(UUID userId, String name);

    List<TagEntity> findAllByUserIdIsNull();
}
