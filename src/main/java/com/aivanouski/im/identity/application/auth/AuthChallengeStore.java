package com.aivanouski.im.identity.application.auth;

import java.time.Instant;
import java.util.Optional;

public interface AuthChallengeStore {
    void save(String phone, String challenge, Instant expiresAt);

    Optional<AuthChallenge> findByPhone(String phone);

    void remove(String phone);
}
