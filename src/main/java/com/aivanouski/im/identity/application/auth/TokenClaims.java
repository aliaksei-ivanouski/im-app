package com.aivanouski.im.identity.application.auth;

import java.util.UUID;

public record TokenClaims(UUID userId, UUID deviceId, long expiresAtEpochSeconds) {
}
