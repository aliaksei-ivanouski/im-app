package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.identity.domain.RefreshTokenRecord;

import java.util.Optional;

public interface RefreshTokenLookupRepository {
    Optional<RefreshTokenRecord> findByTokenHash(String tokenHash);
}
