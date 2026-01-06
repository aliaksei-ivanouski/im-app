package com.aivanouski.im.identity.application.auth;

import java.time.Instant;

public record AuthStartResult(
        String status,
        String challenge,
        Instant challengeExpiresAt,
        Instant otpExpiresAt
) {
    public static AuthStartResult otpRequired(String challenge, Instant challengeExpiresAt, Instant otpExpiresAt) {
        return new AuthStartResult("otp_required", challenge, challengeExpiresAt, otpExpiresAt);
    }
}
