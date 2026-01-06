package com.aivanouski.im.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviceJpaRepository extends JpaRepository<DeviceEntity, UUID> {
    Optional<DeviceEntity> findByIdAndUserId(UUID id, UUID userId);
}
