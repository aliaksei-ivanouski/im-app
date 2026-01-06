package com.aivanouski.im.identity.application.auth;

import java.time.Instant;

public record AuthChallenge(String phone, String challenge, Instant expiresAt) {
}
