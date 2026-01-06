package com.aivanouski.im.identity.presentation.rest;

public record AuthStartResponse(
        String status,
        String challenge,
        String challengeExpiresAt,
        String otpExpiresAt
) {
}
