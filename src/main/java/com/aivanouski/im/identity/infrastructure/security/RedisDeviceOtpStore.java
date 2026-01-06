package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.identity.application.auth.DeviceOtp;
import com.aivanouski.im.identity.application.auth.DeviceOtpStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisDeviceOtpStore implements DeviceOtpStore {
    private static final String KEY_PREFIX = "auth:otp:device:";
    private static final String SEPARATOR = "|";

    private final StringRedisTemplate redisTemplate;

    public RedisDeviceOtpStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(UUID deviceId, DeviceOtp otp) {
        Instant now = Instant.now();
        if (!otp.expiresAt().isAfter(now)) {
            remove(deviceId);
            return;
        }
        Duration ttl = Duration.between(now, otp.expiresAt());
        String value = otp.codeHash() + SEPARATOR + otp.expiresAt().toEpochMilli();
        redisTemplate.opsForValue().set(key(deviceId), value, ttl);
    }

    @Override
    public Optional<DeviceOtp> findByDeviceId(UUID deviceId) {
        String value = redisTemplate.opsForValue().get(key(deviceId));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        int index = value.lastIndexOf(SEPARATOR);
        if (index <= 0 || index == value.length() - 1) {
            remove(deviceId);
            return Optional.empty();
        }
        String hash = value.substring(0, index);
        String expiresRaw = value.substring(index + 1);
        try {
            Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(expiresRaw));
            return Optional.of(new DeviceOtp(hash, expiresAt));
        } catch (NumberFormatException ex) {
            remove(deviceId);
            return Optional.empty();
        }
    }

    @Override
    public void remove(UUID deviceId) {
        redisTemplate.delete(key(deviceId));
    }

    private String key(UUID deviceId) {
        return KEY_PREFIX + deviceId;
    }
}
