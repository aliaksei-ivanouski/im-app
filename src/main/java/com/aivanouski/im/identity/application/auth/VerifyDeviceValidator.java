package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.shared.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class VerifyDeviceValidator {
    public void validate(String phone, UUID deviceId, String otp, String publicKey, String publicKeyAlg, String signature) {
        if (phone == null || phone.isBlank()) {
            throw new ValidationException("phone is required.");
        }
        if (deviceId == null || otp == null || otp.isBlank()) {
            throw new ValidationException("deviceId and otp are required.");
        }
        if (publicKey == null || publicKey.isBlank() || publicKeyAlg == null || publicKeyAlg.isBlank()) {
            throw new ValidationException("devicePublicKey and publicKeyAlg are required.");
        }
        if (signature == null || signature.isBlank()) {
            throw new ValidationException("signature is required.");
        }
    }
}
