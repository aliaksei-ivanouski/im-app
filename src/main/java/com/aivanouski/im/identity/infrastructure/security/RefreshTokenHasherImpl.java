package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.identity.application.auth.RefreshTokenHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class RefreshTokenHasherImpl implements RefreshTokenHasher {
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return BASE64_URL.encodeToString(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash refresh token.", ex);
        }
    }
}
