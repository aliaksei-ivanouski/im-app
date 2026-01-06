package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.shared.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthStartValidator {
    public void validate(String phone, UUID deviceId) {
        if (phone == null || phone.isBlank()) {
            throw new ValidationException("phone is required.");
        }
        if (deviceId == null) {
            throw new ValidationException("deviceId is required.");
        }
    }
}
