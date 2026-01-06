package com.aivanouski.im.identity.application.auth;

import java.util.UUID;

public interface TokenService {
    String createAccessToken(UUID userId, UUID deviceId);

    String createRefreshToken();

    TokenClaims verify(String token);
}
