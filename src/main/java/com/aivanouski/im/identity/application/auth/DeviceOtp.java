package com.aivanouski.im.identity.application.auth;

import java.time.Instant;

public record DeviceOtp(
        String codeHash,
        Instant expiresAt
) {
}
