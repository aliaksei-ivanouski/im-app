package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class VerifyDeviceValidatorTest {
    private final VerifyDeviceValidator validator = new VerifyDeviceValidator();

    @Test
    void acceptsValidPayload() {
        validator.validate(
                "+12025550123",
                UUID.randomUUID(),
                "123456",
                "base64",
                "EC",
                "signature"
        );
    }

    @Test
    void rejectsMissingPhone() {
        assertThrows(ValidationException.class, () -> validator.validate(null, UUID.randomUUID(), "123456", "pk", "EC", "sig"));
    }

    @Test
    void rejectsMissingDeviceOrOtp() {
        assertThrows(ValidationException.class, () -> validator.validate("+12025550123", null, "123456", "pk", "EC", "sig"));
        assertThrows(ValidationException.class, () -> validator.validate("+12025550123", UUID.randomUUID(), " ", "pk", "EC", "sig"));
    }

    @Test
    void rejectsMissingPublicKeyMetadata() {
        assertThrows(ValidationException.class, () -> validator.validate("+12025550123", UUID.randomUUID(), "123456", null, "EC", "sig"));
        assertThrows(ValidationException.class, () -> validator.validate("+12025550123", UUID.randomUUID(), "123456", "pk", " ", "sig"));
    }

    @Test
    void rejectsMissingSignature() {
        assertThrows(ValidationException.class, () -> validator.validate("+12025550123", UUID.randomUUID(), "123456", "pk", "EC", " "));
    }
}
