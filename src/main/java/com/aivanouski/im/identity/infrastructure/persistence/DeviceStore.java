package com.aivanouski.im.identity.infrastructure.persistence;

import com.aivanouski.im.identity.application.auth.DeviceLookupRepository;
import com.aivanouski.im.identity.application.auth.DeviceRepository;
import com.aivanouski.im.identity.domain.DeviceRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceStore implements DeviceRepository, DeviceLookupRepository {
    private final DeviceJpaRepository deviceJpaRepository;

    public DeviceStore(DeviceJpaRepository deviceJpaRepository) {
        this.deviceJpaRepository = deviceJpaRepository;
    }

    @Override
    public Optional<DeviceRecord> findByIdAndUserId(UUID id, UUID userId) {
        return deviceJpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public Optional<DeviceRecord> findById(UUID id) {
        return deviceJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public DeviceRecord save(DeviceRecord device) {
        DeviceEntity saved = deviceJpaRepository.save(toEntity(device));
        return toDomain(saved);
    }

    private DeviceRecord toDomain(DeviceEntity entity) {
        return new DeviceRecord(
                entity.getId(),
                entity.getUserId(),
                entity.getPublicKey(),
                entity.getPublicKeyAlg(),
                entity.getChallenge(),
                entity.getChallengeExpiresAt(),
                entity.getVerifiedAt(),
                entity.getCreatedAt(),
                entity.getLastSeenAt()
        );
    }

    private DeviceEntity toEntity(DeviceRecord device) {
        DeviceEntity entity = new DeviceEntity(
                device.id(),
                device.userId(),
                device.publicKey(),
                device.publicKeyAlg(),
                device.createdAt()
        );
        if (device.challenge() != null || device.challengeExpiresAt() != null) {
            entity.setChallenge(device.challenge(), device.challengeExpiresAt());
        }
        if (device.verifiedAt() != null) {
            entity.setVerified(device.verifiedAt());
        }
        if (device.lastSeenAt() != null) {
            entity.setLastSeenAt(device.lastSeenAt());
        }
        return entity;
    }
}
