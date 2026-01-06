package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.shared.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RefreshTokenValidator {
    public void validate(UUID deviceId, String refreshToken) {
        if (deviceId == null || refreshToken == null || refreshToken.isBlank()) {
            throw new ValidationException("deviceId and refreshToken are required.");
        }
    }
}
