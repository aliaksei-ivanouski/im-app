package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.identity.domain.RefreshTokenRecord;

public interface RefreshTokenRepository {
    RefreshTokenRecord save(RefreshTokenRecord token);
}
