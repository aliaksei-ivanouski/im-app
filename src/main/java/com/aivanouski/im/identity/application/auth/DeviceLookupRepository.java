package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.identity.domain.DeviceRecord;

import java.util.Optional;
import java.util.UUID;

public interface DeviceLookupRepository {
    Optional<DeviceRecord> findByIdAndUserId(UUID id, UUID userId);

    Optional<DeviceRecord> findById(UUID id);
}
