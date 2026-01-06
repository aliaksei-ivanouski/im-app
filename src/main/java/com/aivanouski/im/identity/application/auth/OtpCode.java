package com.aivanouski.im.identity.application.auth;

import java.time.Instant;

public record OtpCode(
        String code,
        Instant expiresAt
) {
}
