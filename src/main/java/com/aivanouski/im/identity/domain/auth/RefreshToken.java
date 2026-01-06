package com.aivanouski.im.identity.domain.auth;

public record RefreshToken(String value, long expiresInSeconds) {
}
