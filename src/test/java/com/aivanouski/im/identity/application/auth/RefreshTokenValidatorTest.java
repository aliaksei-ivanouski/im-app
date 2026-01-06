package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshTokenValidatorTest {
    private final RefreshTokenValidator validator = new RefreshTokenValidator();

    @Test
    void acceptsValidPayload() {
        validator.validate(UUID.randomUUID(), "refresh-token");
    }

    @Test
    void rejectsMissingDeviceId() {
        assertThrows(ValidationException.class, () -> validator.validate(null, "refresh-token"));
    }

    @Test
    void rejectsMissingRefreshToken() {
        assertThrows(ValidationException.class, () -> validator.validate(UUID.randomUUID(), " "));
    }
}
