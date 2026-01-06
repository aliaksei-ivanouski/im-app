package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.identity.application.auth.OtpHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class OtpHasherImpl implements OtpHasher {
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    @Override
    public String hash(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return BASE64_URL.encodeToString(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash OTP.", ex);
        }
    }

    @Override
    public boolean matches(String otp, String hash) {
        return hash(otp).equals(hash);
    }
}
