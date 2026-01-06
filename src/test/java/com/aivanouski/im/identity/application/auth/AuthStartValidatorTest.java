package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthStartValidatorTest {
    private final AuthStartValidator validator = new AuthStartValidator();

    @Test
    void acceptsValidPayload() {
        validator.validate("+12025550123", UUID.randomUUID());
    }

    @Test
    void rejectsMissingPhone() {
        assertThrows(ValidationException.class, () -> validator.validate(" ", UUID.randomUUID()));
    }

    @Test
    void rejectsMissingDeviceId() {
        assertThrows(ValidationException.class, () -> validator.validate("+12025550123", null));
    }
}
