package com.aivanouski.im.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record UserAccount(UUID id, String phoneHash, String phoneEncrypted, Instant createdAt) {
}
