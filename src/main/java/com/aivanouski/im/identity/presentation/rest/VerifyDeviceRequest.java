package com.aivanouski.im.identity.presentation.rest;

import java.util.UUID;

public record VerifyDeviceRequest(
        String phone,
        UUID deviceId,
        String otp,
        String devicePublicKey,
        String publicKeyAlg,
        String signature
) {
}
