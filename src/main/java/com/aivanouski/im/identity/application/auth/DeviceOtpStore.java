package com.aivanouski.im.identity.application.auth;

import java.util.Optional;
import java.util.UUID;

public interface DeviceOtpStore {
    void save(UUID deviceId, DeviceOtp otp);

    Optional<DeviceOtp> findByDeviceId(UUID deviceId);

    void remove(UUID deviceId);
}
