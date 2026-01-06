package com.aivanouski.im.identity.presentation.rest;

public record TokenResponse(
        String accessToken,
        long accessExpiresInSeconds,
        String refreshToken,
        long refreshExpiresInSeconds
) {
}
