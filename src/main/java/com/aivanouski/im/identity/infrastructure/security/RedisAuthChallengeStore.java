package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.identity.application.auth.AuthChallenge;
import com.aivanouski.im.identity.application.auth.AuthChallengeStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class RedisAuthChallengeStore implements AuthChallengeStore {
    private static final String KEY_PREFIX = "auth:challenge:";
    private static final String SEPARATOR = "|";

    private final StringRedisTemplate redisTemplate;

    public RedisAuthChallengeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String phone, String challenge, Instant expiresAt) {
        String key = key(phone);
        Instant now = Instant.now();
        if (!expiresAt.isAfter(now)) {
            redisTemplate.delete(key);
            return;
        }
        Duration ttl = Duration.between(now, expiresAt);
        String value = challenge + SEPARATOR + expiresAt.toEpochMilli();
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public Optional<AuthChallenge> findByPhone(String phone) {
        String value = redisTemplate.opsForValue().get(key(phone));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        int index = value.lastIndexOf(SEPARATOR);
        if (index <= 0 || index == value.length() - 1) {
            redisTemplate.delete(key(phone));
            return Optional.empty();
        }
        String challenge = value.substring(0, index);
        String expiresRaw = value.substring(index + 1);
        try {
            Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(expiresRaw));
            if (Instant.now().isAfter(expiresAt)) {
                redisTemplate.delete(key(phone));
                return Optional.empty();
            }
            return Optional.of(new AuthChallenge(phone, challenge, expiresAt));
        } catch (NumberFormatException ex) {
            redisTemplate.delete(key(phone));
            return Optional.empty();
        }
    }

    @Override
    public void remove(String phone) {
        redisTemplate.delete(key(phone));
    }

    private String key(String phone) {
        return KEY_PREFIX + phone;
    }
}
