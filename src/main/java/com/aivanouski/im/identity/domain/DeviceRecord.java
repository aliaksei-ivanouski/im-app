package com.aivanouski.im.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record DeviceRecord(
        UUID id,
        UUID userId,
        String publicKey,
        String publicKeyAlg,
        String challenge,
        Instant challengeExpiresAt,
        Instant verifiedAt,
        Instant createdAt,
        Instant lastSeenAt
) {
}
