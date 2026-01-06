package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.identity.application.auth.TokenClaims;
import com.aivanouski.im.shared.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    @Test
    void createAndVerifyAccessToken() {
        JwtService service = new JwtService(jwtProperties(3600), new ObjectMapper());
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();

        String token = service.createAccessToken(userId, deviceId);
        TokenClaims claims = service.verify(token);

        assertEquals(userId, claims.userId());
        assertEquals(deviceId, claims.deviceId());
    }

    @Test
    void verifyRejectsInvalidFormat() {
        JwtService service = new JwtService(jwtProperties(3600), new ObjectMapper());

        assertThrows(ValidationException.class, () -> service.verify("not-a-jwt"));
    }

    @Test
    void verifyRejectsInvalidSignature() {
        JwtService service = new JwtService(jwtProperties(3600), new ObjectMapper());
        String token = service.createAccessToken(UUID.randomUUID(), UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 1) + "x";

        assertThrows(ValidationException.class, () -> service.verify(tampered));
    }

    @Test
    void verifyRejectsExpiredToken() {
        JwtService service = new JwtService(jwtProperties(-1), new ObjectMapper());
        String token = service.createAccessToken(UUID.randomUUID(), UUID.randomUUID());

        assertThrows(ValidationException.class, () -> service.verify(token));
    }

    private JwtProperties jwtProperties(long accessTtlSeconds) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret");
        properties.setAccessTtlSeconds(accessTtlSeconds);
        properties.setIssuer("im");
        return properties;
    }
}
