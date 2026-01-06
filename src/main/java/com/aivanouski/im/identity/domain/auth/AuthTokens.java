package com.aivanouski.im.identity.domain.auth;

public record AuthTokens(AccessToken accessToken, RefreshToken refreshToken) {
}
